import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ' http://localhost:8123/api'

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const endpoints = {
  love: '/ai/love_app/chat/sse',
  manus: '/ai/manus/chat',
}

export function buildChatSseUrl(appKey, { message, chatId } = {}) {
  const endpoint = endpoints[appKey]

  if (!endpoint) {
    throw new Error(`Unknown chat app: ${appKey}`)
  }

  const url = new URL(`${API_BASE_URL}${endpoint}`)
  url.searchParams.set('message', message || '')

  if (appKey === 'love' && chatId) {
    url.searchParams.set('chatId', chatId)
  }

  return url.toString()
}

export function connectChatSse(appKey, payload, handlers = {}) {
  const controller = new AbortController()

  readChatSseStream(appKey, payload, handlers, controller.signal)

  return {
    close() {
      controller.abort()
    },
  }
}

async function readChatSseStream(appKey, payload, handlers, signal) {
  try {
    const response = await fetch(buildChatSseUrl(appKey, payload), {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
      },
      signal,
    })

    if (!response.ok) {
      throw new Error(`SSE request failed with status ${response.status}`)
    }

    if (!response.body) {
      throw new Error('ReadableStream is not supported in this browser.')
    }

    handlers.onOpen?.()

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let completedByPayload = false

    while (true) {
      const { value, done } = await reader.read()

      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const result = consumeSseBuffer(buffer, handlers)
      buffer = result.buffer

      if (result.completed) {
        completedByPayload = true
        await reader.cancel()
        break
      }
    }

    buffer += decoder.decode()

    if (!completedByPayload && buffer.trim()) {
      completedByPayload = consumeSseBuffer(`${buffer}\n\n`, handlers).completed
    }

    if (!signal.aborted) {
      handlers.onDone?.()
    }
  } catch (error) {
    if (signal.aborted) {
      return
    }

    handlers.onError?.(error)
  }
}

function consumeSseBuffer(input, handlers) {
  let buffer = input
  let completed = false
  let boundary = findEventBoundary(buffer)

  while (boundary && !completed) {
    const eventBlock = buffer.slice(0, boundary.index)
    buffer = buffer.slice(boundary.index + boundary.length)
    completed = handleSseEventBlock(eventBlock, handlers)
    boundary = findEventBoundary(buffer)
  }

  return { buffer, completed }
}

function findEventBoundary(buffer) {
  const match = buffer.match(/(?:\r\n|\r|\n){2}/)

  if (!match || match.index == null) {
    return null
  }

  return {
    index: match.index,
    length: match[0].length,
  }
}

function handleSseEventBlock(block, handlers) {
  const dataLines = []
  const rawLines = []

  block.split(/\r\n|\r|\n/).forEach((line) => {
    if (!line || line.startsWith(':')) {
      return
    }

    if (line.startsWith('data:')) {
      const value = line.slice(5)
      dataLines.push(value.startsWith(' ') ? value.slice(1) : value)
      return
    }

    if (!/^[a-zA-Z-]+:/.test(line)) {
      rawLines.push(line)
    }
  })

  const data = dataLines.length ? dataLines.join('\n') : rawLines.join('\n')

  if (!data) {
    return false
  }

  if (isDonePayload(data)) {
    return true
  }

  const chunk = normalizeSseChunk(data)

  if (chunk) {
    handlers.onMessage?.(chunk)
  }

  return false
}

function isDonePayload(raw) {
  const text = String(raw ?? '').trim()
  return text === '[DONE]' || text === 'DONE'
}

function normalizeSseChunk(raw) {
  if (raw == null) {
    return ''
  }

  const text = String(raw)

  try {
    const parsed = JSON.parse(text)
    return pickText(parsed) || text
  } catch {
    return text
  }
}

function pickText(value) {
  if (typeof value === 'string') {
    return value
  }

  if (!value || typeof value !== 'object') {
    return ''
  }

  if (typeof value.content === 'string') return value.content
  if (typeof value.text === 'string') return value.text
  if (typeof value.message === 'string') return value.message
  if (typeof value.result === 'string') return value.result
  if (typeof value.output === 'string') return value.output
  if (typeof value.data === 'string') return value.data
  if (typeof value.data === 'object') return pickText(value.data)

  const choice = value.choices?.[0]
  if (typeof choice?.delta?.content === 'string') return choice.delta.content
  if (typeof choice?.message?.content === 'string') return choice.message.content

  return ''
}
