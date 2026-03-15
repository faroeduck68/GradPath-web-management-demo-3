<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-lc-main">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main Area -->
    <div class="flex flex-col flex-1 min-w-0">
      <!-- Top Navbar -->
      <Navbar />

      <!-- Page Content -->
      <main class="flex-1 overflow-y-auto p-5">
        <RouterView />
      </main>
    </div>
  </div>
</template>
