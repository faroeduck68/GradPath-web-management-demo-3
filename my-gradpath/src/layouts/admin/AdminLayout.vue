<script setup lang="ts">
import { ref, provide } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const isCollapsed = ref(false)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

provide('isCollapsed', isCollapsed)
provide('toggleCollapse', toggleCollapse)
</script>

<template>
  <el-container class="h-screen">
    <!-- Left Sidebar -->
    <Sidebar :collapsed="isCollapsed" />

    <!-- Right: Header + Main -->
    <el-container direction="vertical">
      <Header />
      <el-main class="bg-gray-50">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
