<template>
  <div class="sidebar-logo-container" :class="{ collapse }">
    <router-link class="sidebar-logo-link" to="/" :title="collapse ? 'Xinke ERP' : ''">

      <!-- LOGO（已替换为图片） -->
      <span class="brand-mark" aria-hidden="true">
        <img class="brand-mark-img" src="/src/assets/logo/logo.png" alt="logo" />
      </span>

      <!-- 文字 -->
      <span v-if="!collapse" class="brand-copy">
        <strong>Xinke ERP</strong>
        <small>OpsPlatform</small>
      </span>

    </router-link>
  </div>
</template>

<script setup>
defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})
</script>

<style lang="scss" scoped>
/* 外层容器 */
.sidebar-logo-container {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 10px 12px;
  overflow: hidden;
  background: linear-gradient(180deg,
      rgba(255, 255, 255, 0.94),
      rgba(255, 255, 255, 0.76));
  border-bottom: 1px solid rgba(112, 115, 145, 0.08);
}

/* 核心布局：强制一行 */
.sidebar-logo-link {
  width: 100%;
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  /* 防止换行 */
  gap: 10px;
  text-decoration: none;
  color: inherit;
  min-width: 160px;
}

/* LOGO 容器 */
.brand-mark {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* LOGO 图片 */
.brand-mark-img {
  width: 26px;
  height: 26px;
  object-fit: contain;
  display: block;
}

/* 文字区域 */
.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.12;

  strong {
    font-size: 15px;
    font-weight: 850;
    color: #202437;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  small {
    margin-top: 4px;
    font-size: 11px;
    font-weight: 750;
    color: #8a91a6;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* collapse 模式 */
.sidebar-logo-container.collapse {
  justify-content: center;
  padding: 10px 0;

  .sidebar-logo-link {
    justify-content: center;
    min-width: unset;
  }

  .brand-mark {
    width: 36px;
    height: 36px;
  }

  .brand-mark-img {
    width: 28px;
    height: 28px;
  }
}
</style>