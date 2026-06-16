<!-- 文件说明：App.vue，应用根组件，承载路由出口和全局页面样式。 -->
<script setup>
</script>

<template>
  <div class="app">
    <router-view />
  </div>
</template>

<style scoped>
.app {
  width: 100vw;
  height: 100vh;
}
</style>
