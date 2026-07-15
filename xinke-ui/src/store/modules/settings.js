import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'

const {
  sideTheme,
  showSettings,
  navType,
  tagsView,
  tagsViewPersist,
  tagsIcon,
  tagsViewStyle,
  fixedHeader,
  sidebarLogo,
  dynamicTitle,
  footerVisible,
  footerContent
} = defaultSettings

const useSettingsStore = defineStore('settings', {
  state: () => ({
    title: '',
    theme: '#6C5CE7',
    sideTheme,
    showSettings,
    navType,
    tagsView,
    tagsViewPersist,
    tagsIcon,
    tagsViewStyle,
    fixedHeader,
    sidebarLogo,
    dynamicTitle,
    footerVisible,
    footerContent,
    isDark: false
  }),
  actions: {
    changeSetting(data) {
      const { key, value } = data
      if (Object.prototype.hasOwnProperty.call(this, key)) {
        this[key] = value
      }
    },
    setTitle(title) {
      this.title = title
      useDynamicTitle()
    }
  }
})

export default useSettingsStore
