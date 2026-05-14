<script setup>
import { RouterView } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
</script>

<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="workspace">
      <RouterView />
    </main>
  </div>
</template>
