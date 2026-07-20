<template>
  <div class="app-container token-page">
    <section class="token-hero">
      <div>
        <span class="eyebrow">BI Embed Token</span>
        <h2>外部看板 Token 工具</h2>
        <p>一键获取当前登录 token，生成直播追单 BI 嵌入地址，并记录每次获取时间。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" icon="Key" @click="handleGetToken">获取当前Token</el-button>
        <el-button icon="Refresh" @click="loadHistory">刷新记录</el-button>
      </div>
    </section>

    <section class="token-grid">
      <article class="panel token-panel">
        <div class="panel-head">
          <div>
            <h3>当前 Token</h3>
            <p>建议使用只读账号获取，不要长期使用 admin token。</p>
          </div>
          <el-tag type="success" v-if="currentToken">已获取</el-tag>
          <el-tag type="info" v-else>未获取</el-tag>
        </div>

        <el-input
          v-model="tokenDisplay"
          :type="showToken ? 'textarea' : 'text'"
          :rows="showToken ? 5 : 1"
          readonly
          placeholder="点击获取当前Token"
          class="token-input"
        />

        <div class="button-row">
          <el-button icon="View" @click="showToken = !showToken" :disabled="!currentToken">
            {{ showToken ? '隐藏Token' : '显示Token' }}
          </el-button>
          <el-button type="primary" icon="CopyDocument" @click="copyText(currentToken)" :disabled="!currentToken">
            复制Token
          </el-button>
          <el-button type="warning" icon="Delete" plain @click="clearToken" :disabled="!currentToken">
            清空显示
          </el-button>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <h3>BI 嵌入地址</h3>
            <p>复制到外部 BI 的 iframe / Vue 组件里使用。</p>
          </div>
        </div>

        <el-input
          v-model="embedUrl"
          type="textarea"
          :rows="5"
          readonly
          placeholder="获取Token后自动生成"
        />

        <div class="button-row">
          <el-button type="primary" icon="CopyDocument" @click="copyText(embedUrl)" :disabled="!embedUrl">
            复制嵌入地址
          </el-button>
          <el-button icon="Link" @click="openPreview" :disabled="!embedUrl">
            预览
          </el-button>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h3>获取记录</h3>
          <p>本地只记录获取时间、账号和脱敏摘要，不保存完整 token。</p>
        </div>
        <el-button type="danger" plain icon="Delete" @click="clearHistory" :disabled="!historyRows.length">
          清空记录
        </el-button>
      </div>

      <el-table :data="historyRows" border>
        <el-table-column label="获取时间" prop="createdAt" width="180" />
        <el-table-column label="账号" prop="username" width="140" />
        <el-table-column label="Token摘要" prop="tokenMask" min-width="280" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { getToken } from '@/utils/auth'
import { parseTime } from '@/utils/xinke'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const currentToken = ref('')
const showToken = ref(false)
const historyRows = ref([])
const historyKey = 'xinke:bi-token-history'
const userStore = useUserStore()

const tokenDisplay = computed(() => {
  if (!currentToken.value) return ''
  return showToken.value ? currentToken.value : maskToken(currentToken.value)
})

const embedUrl = computed(() => {
  if (!currentToken.value) return ''
  const url = new URL('/live-bi-embed', window.location.origin)
  url.searchParams.set('token', currentToken.value)
  url.searchParams.set('days', '7')
  return url.toString()
})

function handleGetToken() {
  const token = getToken()
  if (!token) {
    proxy.$modal.msgError('当前没有登录Token，请重新登录后再获取')
    return
  }
  currentToken.value = token
  appendHistory(token)
  proxy.$modal.msgSuccess('Token已获取')
}

function appendHistory(token) {
  const rows = readHistory()
  const row = {
    id: `${Date.now()}`,
    createdAt: parseTime(new Date()),
    username: userStore.name || userStore.nickName || '当前用户',
    tokenMask: maskToken(token)
  }
  const nextRows = [row, ...rows].slice(0, 50)
  localStorage.setItem(historyKey, JSON.stringify(nextRows))
  historyRows.value = nextRows
}

function readHistory() {
  try {
    const rows = JSON.parse(localStorage.getItem(historyKey) || '[]')
    if (!Array.isArray(rows)) return []
    const sanitizedRows = rows.map(row => ({
      id: row.id,
      createdAt: row.createdAt,
      username: row.username,
      tokenMask: row.tokenMask || maskToken(row.token || '')
    }))
    localStorage.setItem(historyKey, JSON.stringify(sanitizedRows))
    return sanitizedRows
  } catch (e) {
    return []
  }
}

function loadHistory() {
  historyRows.value = readHistory()
}

function clearToken() {
  currentToken.value = ''
  showToken.value = false
}

function clearHistory() {
  proxy.$modal.confirm('确认清空本地Token获取记录吗？').then(() => {
    localStorage.removeItem(historyKey)
    historyRows.value = []
    proxy.$modal.msgSuccess('已清空')
  }).catch(() => {})
}

async function copyText(text) {
  if (!text) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const input = document.createElement('textarea')
      input.value = text
      input.setAttribute('readonly', 'readonly')
      input.style.position = 'fixed'
      input.style.left = '-9999px'
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    proxy.$modal.msgSuccess('已复制')
  } catch (e) {
    proxy.$modal.msgError('复制失败，请手动复制')
  }
}

function openPreview() {
  if (embedUrl.value) {
    window.open(embedUrl.value, '_blank')
  }
}

function maskToken(token) {
  if (!token) return ''
  if (token.length <= 24) return token
  return `${token.slice(0, 12)}...${token.slice(-12)}`
}

onMounted(loadHistory)
</script>

<style scoped lang="scss">
.token-page {
  display: grid;
  gap: 16px;
}

.token-hero,
.panel {
  border: 1px solid rgba(122, 118, 255, .12);
  border-radius: 16px;
  background: rgba(255, 255, 255, .9);
  box-shadow: 0 18px 42px rgba(80, 86, 160, .1);
}

.token-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background:
    radial-gradient(circle at 86% 0%, rgba(162, 155, 254, .22), transparent 34%),
    rgba(255, 255, 255, .9);

  h2 {
    margin: 8px 0;
    font-size: 28px;
  }

  p {
    margin: 0;
    color: #717a96;
  }
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(108, 92, 231, .1);
  color: #6c5ce7;
  font-weight: 800;
}

.hero-actions,
.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.token-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  min-width: 0;
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 18px;
  }

  p {
    margin: 6px 0 0;
    color: #8790aa;
    font-size: 13px;
  }
}

.token-input {
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    font-family: Consolas, Monaco, monospace;
  }
}

.button-row {
  margin-top: 14px;
}

@media (max-width: 1000px) {
  .token-hero,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .token-grid {
    grid-template-columns: 1fr;
  }
}
</style>
