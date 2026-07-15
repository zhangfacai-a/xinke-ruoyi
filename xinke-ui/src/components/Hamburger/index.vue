<template>
  <button
    class="sidebar-toggle"
    :class="{ 'is-open': isActive }"
    type="button"
    :aria-label="isActive ? '收起侧边栏' : '展开侧边栏'"
    @click="toggleClick"
  >
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4.75 6.75h14.5" />
      <path d="M4.75 12h10.5" />
      <path d="M4.75 17.25h14.5" />
      <path class="chevron" d="m15.25 9.25 2.75 2.75-2.75 2.75" />
    </svg>
  </button>
</template>

<script setup>
defineProps({
  isActive: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggleClick'])
const toggleClick = () => {
  emit('toggleClick')
}
</script>

<style scoped>
.sidebar-toggle {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid rgba(108, 92, 231, 0.10);
  border-radius: 13px;
  color: #6c5ce7;
  background: rgba(108, 92, 231, 0.075);
  cursor: pointer;
  transition: transform 0.18s ease, background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.sidebar-toggle:hover {
  border-color: rgba(108, 92, 231, 0.18);
  background: rgba(108, 92, 231, 0.12);
  box-shadow: 0 8px 18px rgba(108, 92, 231, 0.12);
  transform: translateY(-1px);
}

.sidebar-toggle svg {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.9;
}

.sidebar-toggle .chevron {
  opacity: 0.8;
  transform-origin: center;
  transition: transform 0.18s ease;
}

.sidebar-toggle.is-open .chevron {
  transform: rotate(180deg);
}
</style>
