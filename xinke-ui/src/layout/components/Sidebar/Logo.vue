<template>
  <div class="sidebar-logo-container" :class="{ collapse }">
    <router-link class="sidebar-logo-link" to="/" :title="collapse ? 'Xinke ERP' : ''">

      <!-- LOGO -->
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
  box-sizing: border-box;
  overflow: hidden;

  background: linear-gradient(180deg,
      rgba(255, 255, 255, 0.94),
      rgba(255, 255, 255, 0.76));

  border-bottom: 1px solid rgba(112, 115, 145, 0.08);
}

/* ===== 核心修复区（关键） ===== */
.sidebar-logo-link {
  width: 100%;
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;

  flex-wrap: nowrap !important;
  white-space: nowrap !important;

  gap: 10px;
  text-decoration: none;
  color: inherit;

  min-width: 0;
  overflow: hidden;
}

/* 防止 Element Plus / 全局样式污染 span */
.sidebar-logo-link span {
  display: inline-flex !important;
}

/* LOGO */
.brand-mark {
  width: 34px;
  height: 34px;

  display: flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;
}

/* LOGO图片 */
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
  flex: 1;
  flex-shrink: 0;
  line-height: 1.12;
}

/* 文本不换行 */
.brand-copy strong,
.brand-copy small {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 标题 */
.brand-copy strong {
  font-size: 15px;
  font-weight: 850;
  color: #202437;
}

/* 副标题 */
.brand-copy small {
  margin-top: 4px;
  font-size: 11px;
  font-weight: 750;
  color: #8a91a6;
}

/* collapse 模式 */
.sidebar-logo-container.collapse {
  justify-content: center;
  width: 64px;
  padding: 10px 0;

  .sidebar-logo-link {
    width: 44px;
    min-width: 44px;
    max-width: 44px;
    justify-content: center !important;
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