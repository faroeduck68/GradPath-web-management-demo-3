<script setup lang="ts">
import { inject } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'

const isCollapsed = inject<Ref<boolean>>('isCollapsed')!
const toggleCollapse = inject<() => void>('toggleCollapse')!
const router = useRouter()

function handleCommand(command: string) {
  if (command === 'home') {
    router.push('/')
  }
}
</script>

<template>
  <el-header
    class="flex items-center justify-between"
    style="background: #fff; border-bottom: 1px solid #f0f0f0; height: 56px; padding: 0 20px;"
  >
    <el-icon class="cursor-pointer text-lg" @click="toggleCollapse">
      <Fold v-if="!isCollapsed" />
      <Expand v-else />
    </el-icon>

    <el-dropdown trigger="click" @command="handleCommand">
      <span class="flex items-center gap-2 cursor-pointer text-sm text-gray-600">
        <el-avatar :size="28" style="background-color: #409eff;">A</el-avatar>
        管理端
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="home">
            <el-icon><SwitchButton /></el-icon>
            返回首页
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </el-header>
</template>
