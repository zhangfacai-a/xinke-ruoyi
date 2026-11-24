import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@/assets/styles/index.scss'
import JdCustomers from '@/views/live/jd-customers/index.vue'

createApp(JdCustomers).use(createPinia()).use(ElementPlus, { locale: zhCn }).mount('#app')
