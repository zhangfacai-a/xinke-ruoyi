import { CONFIG } from './config.js'

const LIVE_LIST_URL = 'https://buyin.jinritemai.com/dashboard/compass-home/live-list'

const wait = ms => new Promise(resolve => setTimeout(resolve, ms))

async function requestOnce (path, options) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 15000)
  try {
    const response = await fetch(`${CONFIG.apiBase}${path}`, {
      ...options, signal: controller.signal,
      headers: { 'Content-Type': 'application/json', 'X-Live-Metrics-Key': CONFIG.uploadKey, 'X-Plugin-Version': CONFIG.pluginVersion, ...(options.headers || {}) }
    })
    const json = await response.json().catch(() => null)
    if (!response.ok || !json || Number(json.code) !== 200) {
      const message = json?.msg || `服务请求失败（${response.status}）`
      if (message === 'PLUGIN_VERSION_EXPIRED') throw new Error('插件版本已过期，请安装最新版本')
      if (message === 'PLUGIN_AUTH_FAILED') throw new Error('插件授权失效，请联系管理员')
      const error = new Error(message)
      error.retryable = response.status >= 500 || Number(json?.retryAfter) > 0
      throw error
    }
    return json.data
  } catch (error) {
    if (error?.name === 'AbortError') {
      const timeout = new Error('连接本地服务超时，请确认后台已经启动')
      timeout.retryable = true
      throw timeout
    }
    if (error instanceof TypeError) error.retryable = true
    throw error
  } finally { clearTimeout(timer) }
}

async function api (path, options = {}) {
  let failure
  for (let attempt = 0; attempt < 2; attempt += 1) {
    try { return await requestOnce(path, options) } catch (error) {
      failure = error
      if (!error?.retryable || attempt === 1) throw error
      await wait(600)
    }
  }
  throw failure
}

async function openLiveList () {
  const tabs = await chrome.tabs.query({ url: `${LIVE_LIST_URL}*` })
  const existing = tabs.find(tab => Number.isInteger(tab.id))
  if (existing) {
    await chrome.tabs.update(existing.id, { active: true })
    if (Number.isInteger(existing.windowId)) await chrome.windows.update(existing.windowId, { focused: true })
    return
  }
  await chrome.tabs.create({ url: LIVE_LIST_URL, active: true })
}

chrome.action.onClicked.addListener(async tab => {
  if (tab?.id && /^(https:\/\/(compass|buyin)\.jinritemai\.com\/)/.test(tab.url || '')) {
    const handled = await chrome.tabs.sendMessage(tab.id, { type: 'JRM_UI_TOGGLE' }).catch(() => null)
    if (handled?.handled) return
  }
  await openLiveList()
})

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  ;(async () => {
    if (message?.type === 'JRM_OPEN_LIVE_LIST') { await openLiveList(); return { ok: true } }
    if (message?.type === 'JRM_CONTEXT') {
      const params = new URLSearchParams({ platformSessionId: String(message.platformSessionId || ''), roomName: String(message.roomName || '') })
      return { ok: true, data: await api(`/open-api/live-metrics/v3/context?${params}`) }
    }
    if (message?.type === 'JRM_TRANSITION') return { ok: true, data: await api('/open-api/live-metrics/v3/transition', { method: 'POST', body: JSON.stringify(message.payload) }) }
    return { ok: false, error: '不支持的插件操作' }
  })().then(sendResponse).catch(error => sendResponse({ ok: false, error: error?.message || '插件操作失败' }))
  return true
})
