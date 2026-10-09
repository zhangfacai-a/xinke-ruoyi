import { CONFIG } from './config.js'

const DRAFT_KEY = 'jrm_pending_draft'
const METRIC_KEYS = ['live_show_cnt', 'watch_ucnt', 'pay_ucnt', 'follow_anchor_ucnt', 'content_interaction_cnt']

async function requestJson (url, options = {}, timeoutMs = 15000) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(url, { ...options, signal: controller.signal })
    const json = await response.json().catch(() => null)
    return { response, json }
  } catch (error) {
    if (error?.name === 'AbortError') throw new Error('系统连接超时，请稍后重试')
    throw error
  } finally { clearTimeout(timer) }
}

const normalizeRoomName = value => String(value || '').normalize('NFKC').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ')
  .replace(/\s*(?:(?:直播)?开播时间|直播开始时间)\s*[:：]?\s*20\d{2}[/\.\-]\d{1,2}[/\.\-]\d{1,2}[\s\S]*$/i, '')
  .replace(/\s*已直播\s*\d+\s*(?:天|小时|时|分钟|分|秒)[\s\S]*$/i, '').trim()
const numeric = metric => {
  const raw = String(metric?.value ?? '').replace(/,/g, '').trim()
  if (!raw || ['-', '--', '—', '暂无'].includes(raw)) return 0
  const match = raw.match(/^([0-9.]+)\s*(万|亿)?$/)
  if (!match) throw new Error(`经营数据“${raw}”无法识别，请刷新页面后重试`)
  return Math.round(Number(match[1]) * (match[2] === '亿' ? 100000000 : match[2] === '万' ? 10000 : 1))
}
async function drafts () { return (await chrome.storage.local.get(DRAFT_KEY))[DRAFT_KEY] || [] }
async function saveDraft (value) { await chrome.storage.local.set({ [DRAFT_KEY]: value ? [value] : [] }) }
async function installationId () {
  const key = 'jrm_installation'; const saved = await chrome.storage.local.get(key)
  if (saved[key]) return saved[key]
  const value = crypto.randomUUID(); await chrome.storage.local.set({ [key]: value }); return value
}
function normalizeCapture (record) { return { occurredAt: Number(record.capturedAt) || Date.now(), roomName: normalizeRoomName(record.roomName), liveStartTime: String(record.liveStartTime || '').trim(), pageUrl: String(record.pageUrl || ''), data: { metrics: record.metrics || {} } } }
async function upload (freshAfter = 0) {
  const item = (await drafts()).at(-1)
  if (!item) throw new Error('尚未采集到数据，请先点击读取经营数据')
  if (freshAfter && item.occurredAt <= Number(freshAfter)) throw new Error('没有读取到刷新后的数据，请重新读取')
  if (!item.roomName) throw new Error('没有识别到直播间名称')
  if (!item.liveStartTime) throw new Error('没有识别到直播开始时间')
  const metrics = item.data?.metrics || {}
  if (METRIC_KEYS.some(key => metrics[key] == null)) throw new Error('页面五项经营数据尚未读取完整')
  const body = { roomName: item.roomName, liveStartTime: item.liveStartTime, capturedAt: item.occurredAt, exposureTotal: numeric(metrics.live_show_cnt), watchTotal: numeric(metrics.watch_ucnt), dealTotal: numeric(metrics.pay_ucnt), newFanTotal: numeric(metrics.follow_anchor_ucnt), interactionTotal: numeric(metrics.content_interaction_cnt), idempotencyKey: `${await installationId()}-${item.liveStartTime}-${item.occurredAt}` }
  const { response, json } = await requestJson(`${CONFIG.apiBase}/open-api/live-metrics/v2/submissions`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Live-Metrics-Key': CONFIG.uploadKey, 'X-Plugin-Version': CONFIG.pluginVersion, 'X-Request-Id': crypto.randomUUID() }, body: JSON.stringify(body) })
  if (!json) throw new Error(`系统返回异常（HTTP ${response.status}）`)
  if (!response.ok || Number(json.code) !== 200) { if (json.msg === 'PLUGIN_VERSION_EXPIRED') throw new Error('插件版本已过期，请更新插件'); throw new Error(json.msg || `上传失败（HTTP ${response.status}）`) }
  await saveDraft(null); return { sent: 1, pending: 0, result: json.data }
}
async function handleMessage (message) {
  if (message.type === 'JRM_DOM_CAPTURE') {
    const next = normalizeCapture(message.record || {})
    if (!next.roomName) throw new Error('没有识别到直播间名称')
    if (!next.liveStartTime) throw new Error('没有识别到直播开始时间')
    if (METRIC_KEYS.some(key => next.data.metrics[key] == null)) throw new Error('页面五项经营数据尚未读取完整')
    await saveDraft(next)
    return { ok: true, latest: next, pending: 1 }
  }
  if (message.type === 'JRM_UPLOAD') return { ok: true, ...await upload(message.freshAfter) }
  if (message.type === 'JRM_DISCARD_DRAFT') { await saveDraft(null); return { ok: true, pending: 0 } }
  if (message.type === 'JRM_STATE') { const latest = (await drafts()).at(-1) || null; return { pending: latest ? 1 : 0, version: CONFIG.pluginVersion, latest } }
  return { ok: false, error: '不支持的插件操作' }
}
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => { handleMessage(message).then(sendResponse).catch(error => sendResponse({ ok: false, error: error?.message || '插件后台处理失败' })); return true })
chrome.action.onClicked.addListener(tab => { if (tab?.id) chrome.tabs.sendMessage(tab.id, { type: 'JRM_UI_TOGGLE' }).catch(() => {}) })
