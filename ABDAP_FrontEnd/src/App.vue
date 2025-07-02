<script setup lang="ts">
import { RouterView } from 'vue-router'
import { Check } from '@element-plus/icons-vue'
</script>

<template>
  <div id="app">
    <!-- 路由视图 -->
    <RouterView />
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
}

.logo {
  display: block;
  margin: 0 auto 1rem;
}

h1 {
  color: #409eff;
  margin-bottom: 1rem;
  font-size: 24px;
}
</style>
