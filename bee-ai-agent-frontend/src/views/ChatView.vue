<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, Copy, HeartHandshake, LoaderCircle, RefreshCw, Send, Sparkles, Square } from 'lucide-vue-next'
import { connectChatSse } from '@/api/chat'
import { chatApps } from '@/data/apps'

const route = useRoute()

const appKey = computed(() => route.meta.appKey || 'love')
const app = computed(() => chatApps[appKey.value])
const messages = ref([])
const draft = ref('')
const chatId = ref('')
const isStreaming = ref(false)
const copied = ref(false)
const bottomAnchor = ref(null)
const assistantAvatarTitle = computed(() => `${app.value.title} 默认头像`)

const TYPEWRITER_INTERVAL = 24
const TYPEWRITER_MIN_BATCH_SIZE = 1
const TYPEWRITER_MAX_BATCH_SIZE = 4
const MANUS_STEP_PREVIEW_LIMIT = 260

let activeSource = null
let typewriterTimer = null
let typingMessageId = ''
let pendingChars = []
let isSseFinished = false

watch(
  appKey,
  () => {
    startNewSession()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  closeActiveSource()
  resetTypewriter()
})

function startNewSession() {
  closeActiveSource()
  resetTypewriter()
  messages.value = []
  draft.value = ''
  copied.value = false
  isStreaming.value = false
  chatId.value = createChatId(appKey.value)
}

function createChatId(prefix) {
  const time = Date.now().toString(36)
  const random = Math.random().toString(36).slice(2, 8)
  return `${prefix}-${time}-${random}`
}

function closeActiveSource() {
  if (activeSource) {
    activeSource.close()
    activeSource = null
  }
}

function resetTypewriter() {
  if (typewriterTimer) {
    window.clearInterval(typewriterTimer)
    typewriterTimer = null
  }

  typingMessageId = ''
  pendingChars = []
  isSseFinished = false
}

function copyChatId() {
  if (!chatId.value) return

  navigator.clipboard?.writeText(chatId.value)
  copied.value = true
  window.setTimeout(() => {
    copied.value = false
  }, 1200)
}

function sendSuggestion(text) {
  if (isStreaming.value) return
  draft.value = text
  submitMessage()
}

function submitMessage() {
  const text = draft.value.trim()
  if (!text || isStreaming.value) return

  if (appKey.value === 'manus') {
    submitManusMessage(text)
    return
  }

  submitLoveMessage(text)
}

function submitLoveMessage(text) {
  const userMessage = createMessage('user', text, 'done')
  const assistantMessage = createMessage('assistant', '', 'streaming')

  messages.value.push(userMessage, assistantMessage)
  const assistantMessageId = assistantMessage.id
  draft.value = ''
  isStreaming.value = true
  scrollToBottom()

  let hasReceivedChunk = false

  closeActiveSource()
  resetTypewriter()
  typingMessageId = assistantMessageId
  activeSource = connectChatSse(
    appKey.value,
    {
      message: text,
      chatId: app.value.usesChatId ? chatId.value : undefined,
    },
    {
      onMessage: (chunk) => {
        hasReceivedChunk = true
        enqueueTypewriterText(assistantMessageId, chunk)
      },
      onDone: () => {
        isSseFinished = true
        activeSource = null
        finishAssistantMessageWhenTypingDone(assistantMessageId)
      },
      onError: () => {
        const assistantMessageState = findMessageById(assistantMessageId)

        if (!hasReceivedChunk && !assistantMessageState?.content && pendingChars.length === 0) {
          resetTypewriter()
          updateMessage(assistantMessageId, {
            content: '连接失败，请确认后端服务已启动，且接口允许跨域访问。',
            status: 'error',
          })
          isStreaming.value = false
        } else {
          isSseFinished = true
          finishAssistantMessageWhenTypingDone(assistantMessageId)
        }

        activeSource = null
        scrollToBottom()
      },
    },
  )
}

function submitManusMessage(text) {
  const userMessage = createMessage('user', text, 'done')

  messages.value.push(userMessage)
  draft.value = ''
  isStreaming.value = true
  scrollToBottom()

  let hasReceivedStep = false

  closeActiveSource()
  resetTypewriter()
  activeSource = connectChatSse(
    appKey.value,
    {
      message: text,
    },
    {
      onMessage: (chunk) => {
        const stepContents = splitManusSteps(chunk)
          .map(formatManusStepContent)
          .filter(Boolean)

        if (!stepContents.length) {
          return
        }

        hasReceivedStep = true
        stepContents.forEach((content) => {
          messages.value.push(createMessage('assistant', appendManusStepSpacing(content), 'done'))
        })
        scrollToBottom()
      },
      onDone: () => {
        if (!hasReceivedStep) {
          messages.value.push(createMessage('assistant', '任务已结束，未收到步骤内容。', 'done'))
        }

        isStreaming.value = false
        activeSource = null
        scrollToBottom()
      },
      onError: () => {
        if (!hasReceivedStep) {
          messages.value.push(
            createMessage('assistant', '连接失败，请确认后端服务已启动，且接口允许跨域访问。', 'error'),
          )
        }

        isStreaming.value = false
        activeSource = null
        scrollToBottom()
      },
    },
  )
}

function stopStreaming() {
  if (!isStreaming.value) return

  const streamingMessage = [...messages.value].reverse().find((item) => item.status === 'streaming')
  if (streamingMessage && !streamingMessage.content) {
    streamingMessage.content = '已停止响应。'
  }
  if (streamingMessage) {
    streamingMessage.status = 'stopped'
  }

  closeActiveSource()
  resetTypewriter()
  isStreaming.value = false
}

function enqueueTypewriterText(messageId, chunk) {
  const message = findMessageById(messageId)

  if (!chunk || !message || message.status !== 'streaming') {
    return
  }

  typingMessageId = messageId
  pendingChars.push(...Array.from(chunk))
  startTypewriter()
}

function startTypewriter() {
  if (typewriterTimer) {
    return
  }

  typewriterTimer = window.setInterval(flushTypewriterText, TYPEWRITER_INTERVAL)
  flushTypewriterText()
}

function flushTypewriterText() {
  if (!typingMessageId) {
    resetTypewriter()
    return
  }

  if (pendingChars.length === 0) {
    window.clearInterval(typewriterTimer)
    typewriterTimer = null
    if (isSseFinished) {
      finishAssistantMessage(typingMessageId)
    }
    return
  }

  const batchSize = getTypewriterBatchSize(pendingChars.length)
  const nextText = pendingChars.splice(0, batchSize).join('')
  appendMessageContent(typingMessageId, nextText)
  scrollToBottom()

  if (pendingChars.length === 0 && isSseFinished) {
    window.clearInterval(typewriterTimer)
    typewriterTimer = null
    finishAssistantMessage(typingMessageId)
  }
}

function finishAssistantMessageWhenTypingDone(messageId) {
  if (pendingChars.length > 0) {
    startTypewriter()
    return
  }

  finishAssistantMessage(messageId)
}

function getTypewriterBatchSize(queueLength) {
  if (queueLength > 240) {
    return TYPEWRITER_MAX_BATCH_SIZE
  }

  if (queueLength > 80) {
    return 2
  }

  return TYPEWRITER_MIN_BATCH_SIZE
}

function splitManusSteps(raw) {
  const text = String(raw ?? '').trim()

  if (!text) {
    return []
  }

  const steps = text.split(/(?=Step\s+\d+\s*[:：])/i).filter(Boolean)
  return steps.length ? steps : [text]
}

function formatManusStepContent(raw) {
  const text = compactManusText(raw)

  if (!text) {
    return ''
  }

  if (text.length <= MANUS_STEP_PREVIEW_LIMIT) {
    return text
  }

  const resultMatch = text.match(/^(.*?结果\s*[:：]\s*)([\s\S]*)$/)

  if (!resultMatch) {
    return truncateText(text, MANUS_STEP_PREVIEW_LIMIT)
  }

  const prefix = resultMatch[1].trimEnd()
  const result = resultMatch[2]
  const resultLimit = Math.max(90, MANUS_STEP_PREVIEW_LIMIT - prefix.length - 1)

  return `${prefix} ${truncateText(result, resultLimit)}`
}

function appendManusStepSpacing(content) {
  return `${String(content ?? '').trimEnd()}\n`
}

function compactManusText(raw) {
  return String(raw ?? '')
    .replace(/\\r\\n|\\n|\\r/g, ' ')
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/<[^>]*>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function truncateText(text, maxLength) {
  const value = String(text ?? '').trim()

  if (value.length <= maxLength) {
    return value
  }

  return `${value.slice(0, maxLength).trim()}...`
}

function appendMessageContent(messageId, content) {
  const message = findMessageById(messageId)

  if (!message) {
    return
  }

  updateMessage(messageId, {
    content: `${message.content}${content}`,
  })
}

function finishAssistantMessage(messageId) {
  const message = findMessageById(messageId)

  if (!message) {
    resetTypewriter()
    isStreaming.value = false
    activeSource = null
    return
  }

  updateMessage(messageId, {
    content: message.content || '未收到响应内容。',
    status: 'done',
  })

  isStreaming.value = false
  activeSource = null
  resetTypewriter()
  scrollToBottom()
}

function findMessageById(messageId) {
  return messages.value.find((message) => message.id === messageId)
}

function updateMessage(messageId, patch) {
  const index = messages.value.findIndex((message) => message.id === messageId)

  if (index === -1) {
    return
  }

  messages.value[index] = {
    ...messages.value[index],
    ...patch,
  }
}

function createMessage(role, content, status) {
  return {
    id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    role,
    content,
    status,
    createdAt: new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }),
  }
}

function scrollToBottom() {
  nextTick(() => {
    bottomAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}
</script>

<template>
  <section class="chat-view" :class="`accent-${app.accent}`">
    <header class="topbar chat-topbar">
      <div>
        <p class="topbar-kicker">{{ app.endpointLabel }}</p>
        <h1>{{ app.title }}</h1>
      </div>

      <div class="chat-actions">
        <button class="ghost-button" type="button" @click="copyChatId" :title="copied ? '已复制' : '复制会话 ID'">
          <Copy :size="16" />
          <span>{{ copied ? '已复制' : '会话 ID' }}</span>
        </button>
        <button class="ghost-button" type="button" @click="startNewSession" title="开启新会话">
          <RefreshCw :size="16" />
          <span>新会话</span>
        </button>
      </div>
    </header>

    <div class="session-strip">
      <span>当前会话</span>
      <code>{{ chatId }}</code>
    </div>

    <div class="message-panel">
      <div v-if="!messages.length" class="empty-chat">
        <span class="empty-icon">
          <HeartHandshake v-if="app.key === 'love'" :size="30" />
          <Bot v-else :size="30" />
        </span>
        <h2>{{ app.emptyTitle }}</h2>
        <p>{{ app.emptySubtitle }}</p>
        <div class="suggestions">
          <button
            v-for="suggestion in app.suggestions"
            :key="suggestion"
            type="button"
            @click="sendSuggestion(suggestion)"
          >
            {{ suggestion }}
          </button>
        </div>
      </div>

      <div v-else class="message-list">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="[message.role, `app-${app.key}`]"
        >
          <div v-if="message.role === 'assistant'" class="message-avatar" :title="assistantAvatarTitle">
            <HeartHandshake v-if="app.key === 'love'" :size="17" />
            <Sparkles v-else :size="17" />
          </div>
          <div class="message-bubble" :class="message.status">
            <p v-if="message.content">
              <span>{{ message.content }}</span>
              <span v-if="message.status === 'streaming'" class="typing-cursor" aria-hidden="true"></span>
            </p>
            <div v-else class="typing-line">
              <LoaderCircle :size="16" class="spin" />
              <span>正在思考...</span>
            </div>
            <time>{{ message.createdAt }}</time>
          </div>
        </article>
        <div ref="bottomAnchor" class="bottom-anchor" />
      </div>
    </div>

    <form class="composer" @submit.prevent="submitMessage">
      <textarea
        v-model="draft"
        :placeholder="app.placeholder"
        rows="1"
        @keydown.enter.exact.prevent="submitMessage"
      />
      <button v-if="isStreaming" class="send-button stop" type="button" title="停止响应" @click="stopStreaming">
        <Square :size="18" fill="currentColor" />
      </button>
      <button v-else class="send-button" type="submit" title="发送消息" :disabled="!draft.trim()">
        <Send :size="18" />
      </button>
    </form>
  </section>
</template>
