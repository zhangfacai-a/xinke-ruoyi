<template>
  <router-view />
</template>

<script setup>
import { handleThemeStyle } from '@/utils/theme'

onMounted(() => {
  nextTick(() => {
    // 初始化主题样式
    handleThemeStyle('#F26B21')
  })
})
</script>
