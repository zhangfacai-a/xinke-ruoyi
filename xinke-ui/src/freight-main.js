import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@/assets/styles/index.scss'
import FreightCollection from '@/views/live/freight/index.vue'
import useUserStore from '@/store/modules/user'
import { getToken } from '@/utils/auth'

const app = createApp(FreightCollection)
const pinia = createPinia()
app.use(pinia)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')

if (getToken()) {
  fetch('/prod-api/getInfo', { headers: { Authorization: `Bearer ${getToken()}` } })
    .then(response => response.json())
    .then(result => {
      if (result.code === 200 && result.user) {
        const user = useUserStore(pinia)
        user.name = result.user.userName || ''
        user.nickName = result.user.nickName || ''
      }
    })
    .catch(() => {})
}
