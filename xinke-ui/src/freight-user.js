import { getToken } from '@/utils/auth'
import { reactive } from 'vue'

const user = reactive({ token: getToken(), name: '', nickName: '' })
export default function useUserStore() { return user }
