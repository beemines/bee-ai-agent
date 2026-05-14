<script setup>
import { RouterLink } from 'vue-router'
import { ArrowRight, Bot, HeartHandshake, Sparkles } from 'lucide-vue-next'
import { appList } from '@/data/apps'

function iconFor(appKey) {
  return appKey === 'love' ? HeartHandshake : Bot
}
</script>

<template>
  <section class="home-view">
    <header class="topbar">
      <div>
        <p class="topbar-kicker">Bee AI Agent</p>
        <h1>应用主页</h1>
      </div>
      <div class="topbar-pill">
        <Sparkles :size="16" />
        <span>选择应用开始对话</span>
      </div>
    </header>

    <div class="home-center">
      <p class="home-kicker">智能体工作台</p>
      <h2>有什么我能帮你的吗？</h2>
      <p class="home-copy">选择一个应用进入聊天室，开启探索之旅吧~</p>

      <div class="app-card-grid">
        <RouterLink
          v-for="app in appList"
          :key="app.key"
          class="app-card"
          :class="`accent-${app.accent}`"
          :to="app.route"
        >
          <span class="app-card-icon">
            <component :is="iconFor(app.key)" :size="22" stroke-width="2" />
          </span>
          <span class="app-card-content">
            <strong>{{ app.title }}</strong>
            <span>{{ app.description }}</span>
            <small>{{ app.endpointLabel }}</small>
          </span>
          <ArrowRight class="app-card-arrow" :size="18" stroke-width="2" />
        </RouterLink>
      </div>
    </div>
  </section>
</template>
