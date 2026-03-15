<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  LayoutDashboard,
  Radar,
  Code2,
  Mic,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next'

const route = useRoute()
const collapsed = ref(true)

const navItems = [
  { icon: LayoutDashboard, label: '首页大盘', to: '/' },
  { icon: Radar, label: '岗位雷达', to: '/jobs' },
  { icon: Code2, label: '算法训练', to: '/algo' },
  { icon: Mic, label: 'AI 面试', to: '/interview' },
]

function isActive(path: string) {
  return route.path === path
}
</script>

<template>
  <aside
    class="flex flex-col h-screen bg-lc-panel border-r border-lc-border transition-all duration-200"
    :class="collapsed ? 'w-16' : 'w-52'"
  >
    <!-- Logo -->
    <div class="flex items-center h-14 px-4 border-b border-lc-border">
      <div class="w-7 h-7 rounded-lg bg-lc-orange flex items-center justify-center shrink-0">
        <span class="text-sm font-bold text-lc-main">G</span>
      </div>
      <transition name="fade">
        <span v-if="!collapsed" class="ml-3 text-sm font-semibold text-lc-text whitespace-nowrap">
          GradPath
        </span>
      </transition>
    </div>

    <!-- Nav Items -->
    <nav class="flex-1 py-3 px-2 space-y-1">
      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="group relative flex items-center h-10 rounded-md px-3 transition-colors"
        :class="
          isActive(item.to)
            ? 'bg-lc-hover text-lc-orange'
            : 'text-lc-gray hover:text-lc-text hover:bg-lc-hover'
        "
      >
        <component :is="item.icon" :size="20" class="shrink-0" />
        <transition name="fade">
          <span v-if="!collapsed" class="ml-3 text-sm whitespace-nowrap">{{ item.label }}</span>
        </transition>
        <!-- Tooltip when collapsed -->
        <div
          v-if="collapsed"
          class="absolute left-full ml-2 px-2 py-1 rounded bg-lc-highlight text-lc-text text-xs whitespace-nowrap opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity z-50"
        >
          {{ item.label }}
        </div>
      </router-link>
    </nav>

    <!-- Collapse Toggle -->
    <button
      class="flex items-center justify-center h-10 mx-2 mb-3 rounded-md text-lc-gray hover:text-lc-text hover:bg-lc-hover transition-colors"
      @click="collapsed = !collapsed"
    >
      <ChevronLeft v-if="!collapsed" :size="18" />
      <ChevronRight v-else :size="18" />
    </button>
  </aside>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
