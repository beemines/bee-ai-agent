<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Bot, HeartHandshake, Home, Server } from 'lucide-vue-next'
import { API_BASE_URL } from '@/api/chat'
import { appList } from '@/data/apps'

const route = useRoute()

const navItems = computed(() => [
  {
    title: '应用主页',
    to: '/',
    icon: Home,
  },
  ...appList.map((app) => ({
    title: app.navTitle,
    to: app.route,
    icon: app.key === 'love' ? HeartHandshake : Bot,
  })),
])
</script>

<template>
  <aside class="sidebar">
    <RouterLink class="brand" to="/" aria-label="返回应用主页">
      <img src="../asset/logo.ico" class="brand-avatar" alt="logo"/>
      <span class="brand-name">Bee-Agent</span>
    </RouterLink>

    <nav class="nav-list" aria-label="应用导航">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="nav-item"
        :class="{ active: route.path === item.to }"
      >
        <component :is="item.icon" :size="18" stroke-width="2" />
        <span>{{ item.title }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-footer">
      <Server :size="16" stroke-width="2" />
      <span>Bee-Ai-Agent</span>
    </div>
  </aside>
</template>
