<template>
  <div class="navbar" :class="'nav' + settingsStore.navType">
    <div class="sidebar-toggle-wrap">
      <hamburger :is-active="appStore.sidebar.opened" @toggleClick="toggleSideBar" />
    </div>

    <top-nav v-if="settingsStore.navType == 2" id="topmenu-container" class="topmenu-container" />
    <template v-if="settingsStore.navType == 3">
      <logo v-show="settingsStore.sidebarLogo" :collapse="false" />
      <top-bar id="topbar-container" class="topbar-container" />
    </template>

    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <!-- <header-search id="header-search" class="right-menu-item" />
        <screenfull id="screenfull" class="right-menu-item hover-effect" /> -->

        <el-tooltip content="消息通知" effect="dark" placement="bottom">
          <header-notice id="header-notice" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="hover" @command="handleCommand">
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
          <span class="user-nickname">{{ userStore.nickName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item command="lockScreen">
              <span>锁定屏幕</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import TopNav from './TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import HeaderSearch from '@/components/HeaderSearch'
import HeaderNotice from './HeaderNotice'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const lockStore = useLockStore()
const settingsStore = useSettingsStore()

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command) {
  switch (command) {
    case 'lockScreen':
      lockScreen()
      break
    case 'logout':
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/index'
    })
  }).catch(() => {})
}

function lockScreen() {
  lockStore.lockScreen(route.fullPath)
  router.push('/lock')
}
</script>

<style lang="scss" scoped>
.navbar.nav3 {
  .sidebar-toggle-wrap {
    display: none !important;
  }
}

.navbar {
  height: var(--navbar-height, 54px);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
  background: var(--navbar-bg);
  box-shadow: none;

  .sidebar-toggle-wrap {
    width: 38px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 38px;
  }

  .toolbar-context {
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
  }

  .toolbar-row {
    display: flex;
    align-items: center;
    gap: 9px;
    min-width: 0;
  }

  .toolbar-title {
    max-width: 180px;
    overflow: hidden;
    color: #1f2433;
    font-size: 15px;
    font-weight: 650;
    letter-spacing: 0;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .live-pill {
    height: 18px;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 0 8px;
    border-radius: 3px;
    color: #287a52;
    background: #edf7f1;
    font-size: 11px;
    font-weight: 800;
    white-space: nowrap;

    &::before {
      content: "";
      width: 6px;
      height: 6px;
      border-radius: 999px;
      background: #00b894;
      box-shadow: none;
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 4px;
  }

  .right-menu {
    height: 100%;
    line-height: var(--navbar-height, 54px);
    display: flex;
    align-items: center;
    gap: 7px;
    margin-left: auto;

    &:focus {
      outline: none;
    }

    .today-chip {
      height: 32px;
      display: inline-flex;
      align-items: center;
      padding: 0 12px;
      border-radius: 3px;
      color: #66727f;
      background: #f3f5f7;
      font-size: 12px;
      font-weight: 750;
      white-space: nowrap;
      box-shadow: inset 0 0 0 1px #e3e7eb;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      padding: 0;
      border-radius: 12px;
      color: #697189;
      font-size: 17px;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.2s, color 0.2s, transform 0.2s;

        &:hover {
          background: #fff1e9;
          color: #f26b21;
          transform: translateY(-1px);
        }
      }
    }

    .avatar-container {
      width: auto;
      max-width: 132px;
      margin-right: 0;
      padding: 0 9px 0 7px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.78);
      box-shadow: inset 0 0 0 1px rgba(112, 115, 145, 0.12), 0 7px 18px rgba(55, 62, 97, 0.05);

      .avatar-wrapper {
        height: 34px;
        min-width: 0;
        position: relative;
        right: 0;
        display: flex;
        align-items: center;
        margin-top: 0;

        .user-avatar {
          width: 26px;
          height: 26px;
          flex: 0 0 26px;
          margin-right: 7px;
          border-radius: 50%;
          cursor: pointer;
        }

        .user-nickname {
          min-width: 0;
          overflow: hidden;
          position: relative;
          left: 0;
          bottom: 0;
          color: #2f3548;
          font-size: 13px;
          font-weight: 700;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}

@media (max-width: 900px) {
  .navbar {
    .toolbar-title {
      max-width: 120px;
    }

    .today-chip {
      display: none !important;
    }
  }
}
</style>
