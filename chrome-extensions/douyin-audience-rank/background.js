importScripts('config.js')

const TARGET_ORIGIN = 'https://anchor.douyin.com'
const TARGET_PATH = '/anchor/analyze/audience_rank'
const UPLOAD_HEADER = 'X-Audience-Upload-Key'
const ALLOWED_ERP_PATHS = new Set([
  '/open-api/douyin/audience-rank/ping',
  '/open-api/douyin/audience-rank/import'
])

function getErpConfig() {
  const value = globalThis.AUDIENCE_RANK_CONFIG
  const erpBaseUrl = String(value?.erpBaseUrl || '').trim().replace(/\/$/, '')
  const uploadKey = String(value?.uploadKey || '').trim()
  if (!erpBaseUrl || !uploadKey) throw new Error('插件缺少内置连接配置')
  return { erpBaseUrl, uploadKey }
}

async function requestErp(message) {
  const path = String(message?.path || '')
  const method = String(message?.method || 'GET').toUpperCase()
  if (!ALLOWED_ERP_PATHS.has(path) || !['GET', 'POST'].includes(method)) {
    throw new Error('不允许的 ERP 请求')
  }

  const { erpBaseUrl, uploadKey } = getErpConfig()
  const controller = new AbortController()
  const timeoutMs = method === 'POST' ? 180000 : 15000
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(`${erpBaseUrl}${path}`, {
      method,
      body: method === 'POST' ? message.body : undefined,
      cache: 'no-store',
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        [UPLOAD_HEADER]: uploadKey,
        ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {})
      }
    })
    const text = await response.text()
    let body = null
    if (text) {
      try {
        body = JSON.parse(text)
      } catch {
        throw new Error(`ERP 没有返回 JSON（HTTP ${response.status}）`)
      }
    }
    if (!response.ok) {
      const error = new Error(`ERP 请求失败：${body?.msg || body?.message || `HTTP ${response.status}`}`)
      error.httpStatus = response.status
      throw error
    }
    return { ok: true, body }
  } catch (error) {
    const timeoutMessage = error?.name === 'AbortError'
      ? (method === 'POST'
          ? '上传处理超时，数据可能已经保存，请到系统上传记录中确认后再重试'
          : 'ERP 连接超时，请检查地址和服务状态')
      : String(error?.message || error || 'ERP 请求失败')
    return { ok: false, error: timeoutMessage, httpStatus: error?.httpStatus || null }
  } finally {
    clearTimeout(timeout)
  }
}

function isTargetPage(value) {
  try {
    const url = new URL(value)
    return url.origin === TARGET_ORIGIN && url.pathname === TARGET_PATH
  } catch {
    return false
  }
}

async function updateAction(tabId, url) {
  if (!Number.isInteger(tabId)) return
  await chrome.action.setPopup({ tabId, popup: isTargetPage(url) ? '' : 'popup.html' })
}

async function updateAllTabs() {
  const tabs = await chrome.tabs.query({})
  await Promise.all(tabs.map((tab) => updateAction(tab.id, tab.url).catch(() => {})))
}

chrome.runtime.onInstalled.addListener(updateAllTabs)
chrome.runtime.onStartup.addListener(updateAllTabs)
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url || changeInfo.status === 'complete') updateAction(tabId, tab.url).catch(() => {})
})
chrome.tabs.onActivated.addListener(async ({ tabId }) => {
  try {
    const tab = await chrome.tabs.get(tabId)
    await updateAction(tabId, tab.url)
  } catch {
    // The tab may have closed before its action state was updated.
  }
})

chrome.action.onClicked.addListener(async (tab) => {
  if (!tab?.id || !isTargetPage(tab.url)) return
  try {
    await chrome.tabs.sendMessage(tab.id, { type: 'XINKE_AUDIENCE_TOGGLE' })
  } catch {
    await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] })
    await chrome.tabs.sendMessage(tab.id, { type: 'XINKE_AUDIENCE_TOGGLE' })
  }
})

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type !== 'XINKE_AUDIENCE_ERP_REQUEST') return false
  requestErp(message).then(sendResponse).catch((error) => {
    sendResponse({ ok: false, error: String(error?.message || error || 'ERP 请求失败') })
  })
  return true
})

updateAllTabs().catch(() => {})
