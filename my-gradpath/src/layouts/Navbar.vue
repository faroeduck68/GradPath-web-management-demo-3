<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronRight, Wifi } from 'lucide-vue-next'

const route = useRoute()

const breadcrumbs = computed(() => {
  const nameMap: Record<string, string> = {
    home: '首页大盘',
    jobs: '岗位雷达',
    algo: '算法训练',
    interview: 'AI 面试',
  }
  const crumbs = [{ label: 'GradPath', to: '/' }]
  if (route.name && route.name !== 'home') {
    crumbs.push({ label: nameMap[route.name as string] || String(route.name), to: route.path })
  }
  return crumbs
})
</script>

<template>
  <header
    class="flex items-center justify-between h-14 px-5 border-b border-lc-border bg-lc-panel/80 backdrop-blur-md"
  >
    <!-- Breadcrumb -->
    <nav class="flex items-center text-sm">
      <template v-for="(crumb, i) in breadcrumbs" :key="crumb.to">
        <ChevronRight v-if="i > 0" :size="14" class="mx-1.5 text-lc-gray" />
        <router-link
          :to="crumb.to"
          class="transition-colors"
          :class="i === breadcrumbs.length - 1 ? 'text-lc-text' : 'text-lc-gray hover:text-lc-text'"
        >
          {{ crumb.label }}
        </router-link>
      </template>
    </nav>

    <!-- Right Section -->
    <div class="flex items-center gap-4">
      <!-- System Status Indicator -->
      <div class="flex items-center gap-1.5 text-xs text-lc-gray">
        <Wifi :size="14" class="text-lc-green" />
        <span>Redis</span>
      </div>

      <!-- User Avatar -->
      <div
        class="w-8 h-8 rounded-full bg-lc-hover border border-lc-border flex items-center justify-center text-xs text-lc-gray cursor-pointer hover:border-lc-orange transition-colors"
      >
        U
      </div>
    </div>
  </header>
</template>
