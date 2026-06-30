<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component v-if="!route.meta.link" :is="Component" :key="route.path"/>
        </keep-alive>
      </transition>
    </router-view>
    <iframe-toggle />
    <copyright />
  </section>
</template>

<script setup>
import copyright from "./Copyright/index"
import iframeToggle from "./IframeToggle/index"
import useTagsViewStore from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

onMounted(() => {
  addIframe()
})

watchEffect(() => {
  addIframe()
})

function addIframe() {
  if (route.meta.link) {
    useTagsViewStore().addIframeView(route)
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - var(--navbar-height, 64px));
  width: 100%;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 8%, rgba(162, 155, 254, 0.16), transparent 28%),
    radial-gradient(circle at 88% 0%, rgba(108, 92, 231, 0.10), transparent 26%);
}

.app-main::before {
  content: "";
  position: fixed;
  right: 34px;
  bottom: 32px;
  width: 210px;
  height: 210px;
  border-radius: 48px;
  background:
    linear-gradient(135deg, rgba(108, 92, 231, 0.08), rgba(162, 155, 254, 0.05));
  transform: rotate(18deg);
  pointer-events: none;
  z-index: 0;
}

.app-main > * {
  position: relative;
  z-index: 1;
}

.fixed-header + .app-main {
  overflow-y: auto;
  scrollbar-gutter: auto;
  height: calc(100vh - var(--navbar-height, 64px));
  min-height: 0px;
}

.app-main:has(.copyright) {
  padding-bottom: 36px;
}

.fixed-header + .app-main {
  margin-top: var(--navbar-height, 64px);
}

.hasTagsView {
  .app-main {
    min-height: calc(100vh - var(--navbar-height, 64px) - var(--tags-height, 40px));
  }

  .fixed-header + .app-main {
    margin-top: calc(var(--navbar-height, 64px) + var(--tags-height, 40px));
    height: calc(100vh - var(--navbar-height, 64px) - var(--tags-height, 40px));
    min-height: 0px;
  }
}

/* 移动端fixed-header优化 */
@media screen and (max-width: 991px) {
  .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }

  .hasTagsView .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }
}

@supports (-webkit-touch-callout: none) {
  @media screen and (max-width: 991px) {
    .fixed-header + .app-main {
      padding-bottom: max(17px, calc(constant(safe-area-inset-bottom) + 10px));
      padding-bottom: max(17px, calc(env(safe-area-inset-bottom) + 10px));
      height: calc(100svh - var(--navbar-height, 64px));
      height: calc(100dvh - var(--navbar-height, 64px));
    }

    .hasTagsView .fixed-header + .app-main {
      padding-bottom: max(17px, calc(constant(safe-area-inset-bottom) + 10px));
      padding-bottom: max(17px, calc(env(safe-area-inset-bottom) + 10px));
      height: calc(100svh - var(--navbar-height, 64px) - var(--tags-height, 40px));
      height: calc(100dvh - var(--navbar-height, 64px) - var(--tags-height, 40px));
    }
  }
}
</style>

<style lang="scss">
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}
</style>
