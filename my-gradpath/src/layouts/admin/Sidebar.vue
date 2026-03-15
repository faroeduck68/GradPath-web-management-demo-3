<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const menuItems = [
  {
    index: '/admin/dashboard',
    icon: 'Odometer',
    title: '数据大盘',
  },
  {
    title: '岗位管理',
    icon: 'Monitor',
    children: [
      { index: '/admin/jobs/spider', title: '爬虫控制台' },
      { index: '/admin/jobs/review', title: '面经与岗位审核' },
    ],
  },
  {
    index: '/admin/algorithm/questions',
    icon: 'Document',
    title: '算法题库管理',
  },
  {
    index: '/admin/interview/monitor',
    icon: 'Microphone',
    title: 'AI 面试监控',
  },
]

function handleMenuSelect(index: string) {
  router.push(index)
}
</script>

<template>
  <el-aside :width="collapsed ? '64px' : '210px'" class="admin-sidebar">
    <!-- Logo -->
    <div class="flex items-center h-14 px-4" style="border-bottom: 1px solid rgba(255,255,255,0.08);">
      <div class="w-8 h-8 rounded-lg bg-blue-500 flex items-center justify-center shrink-0">
        <span class="text-sm font-bold text-white">G</span>
      </div>
      <span v-if="!collapsed" class="ml-3 text-sm font-semibold text-white whitespace-nowrap">
        GradPath Admin
      </span>
    </div>

    <!-- Menu -->
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      background-color="#001529"
      text-color="rgba(255,255,255,0.65)"
      active-text-color="#409eff"
      :collapse-transition="false"
      @select="handleMenuSelect"
    >
      <template v-for="item in menuItems" :key="item.index || item.title">
        <!-- Sub-menu (岗位管理) -->
        <el-sub-menu v-if="item.children" :index="item.title">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.index"
            :index="child.index"
          >
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>

        <!-- Normal menu item -->
        <el-menu-item v-else :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </el-aside>
</template>

<style scoped>
.admin-sidebar {
  background-color: #001529;
  transition: width 0.3s;
}
.admin-sidebar :deep(.el-menu) {
  border-right: none;
}
</style>
