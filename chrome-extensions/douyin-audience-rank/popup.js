const TARGET_URL = 'https://anchor.douyin.com/anchor/analyze/audience_rank?from=default'
const TARGET_ORIGIN = 'https://anchor.douyin.com'
const TARGET_PATH = '/anchor/analyze/audience_rank'
const API_PATH = '/anchor_pc_tinker_proxy/lego/native/webcast_api/anchor/public/rank'
const PING_PATH = '/open-api/douyin/audience-rank/ping'
const IMPORT_PATH = '/open-api/douyin/audience-rank/import'
const SESSION_KEY = 'douyinAudienceRankCapture'
const UPLOAD_HEADER = 'X-Audience-Upload-Key'
const RESULT_TTL_MS = 30 * 60 * 1000
const IS_PANEL = new URLSearchParams(location.search).get('panel') === '1'
const DEMO_MODE = ['127.0.0.1', 'localhost'].includes(location.hostname) &&
  new URLSearchParams(location.search).has('demo')

if (IS_PANEL) document.documentElement.classList.add('panel-mode')

const elements = Object.fromEntries(
  [...document.querySelectorAll('[id]')].map((element) => [element.id, element])
)

let activeTab = null
let roomName = ''
const settings = validateBuiltInConfig(globalThis.AUDIENCE_RANK_CONFIG)
let capture = null
let diagnosticRaw = null
let reading = false
let uploading = false
let uploadFailed = false
let pageReady = false
let connectionVerified = false
let connectionError = ''

document.addEventListener('DOMContentLoaded', DEMO_MODE ? initializeDemo : initialize)

if (!DEMO_MODE) {
  elements.goTarget.addEventListener('click', goToTargetPage)
  elements.fetchRanks.addEventListener('click', runOneClick)
  elements.refreshRanks.addEventListener('click', runOneClick)
  elements.upload.addEventListener('click', uploadCapture)
  elements.openErpAfterUpload.addEventListener('click', openErp)
  elements.copyJson.addEventListener('click', copyJson)
  elements.downloadJson.addEventListener('click', downloadJson)
  elements.retryConnection.addEventListener('click', checkBuiltInConnection)
}

if (IS_PANEL) {
  elements.panelControls.classList.remove('hidden')
  elements.headerHint.textContent = '拖动此处可移动'
  elements.collapsePanel.addEventListener('click', (event) => {
    event.stopPropagation()
    window.parent.postMessage({ source: 'xinke-audience-panel', type: 'COLLAPSE' }, '*')
  })
  elements.closePanel.addEventListener('click', (event) => {
    event.stopPropagation()
    window.parent.postMessage({ source: 'xinke-audience-panel', type: 'CLOSE' }, '*')
  })
  elements.mainView.querySelector('.topbar').addEventListener('pointerdown', (event) => {
    if (event.button !== 0 || event.target.closest('button')) return
    window.parent.postMessage({
      source: 'xinke-audience-panel', type: 'DRAG_START', screenX: event.screenX, screenY: event.screenY
    }, '*')
  })
  window.addEventListener('message', (event) => {
    if (event.data?.source !== 'xinke-audience-host' || event.data.type !== 'COLLAPSED') return
    document.documentElement.classList.toggle('collapsed', Boolean(event.data.value))
    elements.collapsePanel.textContent = event.data.value ? '+' : '−'
    elements.collapsePanel.title = event.data.value ? '展开悬浮窗' : '收起悬浮窗'
  })
}

function initializeDemo() {
  const mode = new URLSearchParams(location.search).get('demo') || 'result'
  connectionVerified = true
  roomName = '小鑫在线'
  pageReady = true
  showReadyPage()
  elements.anchorName.textContent = roomName
  elements.anchorName.title = roomName
  setPageStatus('success', '页面已就绪')
  updateConnectionState()

  if (mode === 'wrong') {
    showWrongPage()
    return
  }
  const emptyMode = mode === 'empty'
  const commentEnd = emptyMode ? 0 : 126
  const watchStart = emptyMode ? 1 : 44
  const watchEnd = emptyMode ? 0 : 243
  const rows = (start, end) => Array.from(
    { length: Math.max(0, end - start + 1) },
    (_, index) => ({ secUid: `demo-${start + index}` })
  )
  capture = {
    schemaVersion: 2,
    roomName,
    capturedAt: Date.now(),
    commentDataDate: mode === 'warning'
      ? formatDataDate(addLocalDays(new Date(), -2))
      : formatDataDate(addLocalDays(new Date(), -1)),
    watchDataDate: formatDataDate(new Date()),
    commentRanks: rows(1, commentEnd),
    watchRanks: rows(watchStart, watchEnd),
    mergedCount: emptyMode ? 0 : 243,
    raw: {
      comment: { code: 0, data: { ranks: '[演示数据]' } },
      watch: { code: 0, data: { ranks: '[演示数据]' } }
    },
    uploadState: mode === 'success'
      ? { type: 'success', kind: 'success', message: '上传成功，已归入“小鑫在线”。', needsErp: false }
      : null
  }
  renderCapture()
  hideNotice(elements.readStatus)
}

async function initialize() {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
    activeTab = tab || null

    if (isTargetPage(activeTab?.url) && !IS_PANEL) {
      await chrome.tabs.sendMessage(activeTab.id, { type: 'XINKE_AUDIENCE_TOGGLE' })
      window.close()
      return
    }
    if (!isTargetPage(activeTab?.url)) {
      showWrongPage()
    } else {
      await prepareTargetPage()
    }
  } catch (error) {
    if (isTargetPage(activeTab?.url)) {
      showReadyPage()
      markRoomUnavailable(normalizeReadError(error))
    } else {
      showWrongPage()
    }
  }

  updateConnectionState()
  updateReadButtons()
  updateUploadButton()
  await checkBuiltInConnection()
}

async function prepareTargetPage() {
  showReadyPage()
  pageReady = false
  setPageStatus('pending', '识别中')
  elements.anchorName.textContent = '正在识别'

  try {
    roomName = validateRoomName(await readRoomName())
    elements.anchorName.textContent = roomName
    elements.anchorName.title = roomName
    pageReady = true
    setPageStatus('success', '页面已就绪')
    await restoreCapture()
  } catch (error) {
    markRoomUnavailable(normalizeReadError(error))
  }
}

function markRoomUnavailable(message) {
  pageReady = false
  roomName = ''
  elements.anchorName.textContent = '未识别到直播间'
  elements.anchorName.title = ''
  setPageStatus('danger', '需要刷新')
  showNotice(elements.readStatus, 'error', message)
}

function isTargetPage(value) {
  try {
    const url = new URL(value)
    return url.origin === TARGET_ORIGIN && url.pathname === TARGET_PATH
  } catch {
    return false
  }
}

function showWrongPage() {
  pageReady = false
  elements.wrongPage.classList.remove('hidden')
  elements.readyPage.classList.add('hidden')
}

function showReadyPage() {
  elements.wrongPage.classList.add('hidden')
  elements.readyPage.classList.remove('hidden')
}

function setPageStatus(type, text) {
  elements.pageBadge.className = `status-chip ${type}`
  elements.pageBadge.innerHTML = '<i></i>'
  elements.pageBadge.append(document.createTextNode(text))
}

async function goToTargetPage() {
  await chrome.tabs.create({ url: TARGET_URL })
  window.close()
}

async function readRoomName() {
  if (!activeTab?.id) return '当前直播间'

  try {
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: activeTab.id, frameIds: [0] },
      world: 'ISOLATED',
      func: () => {
        const clean = (value) => String(value || '').replace(/\s+/g, ' ').trim()
        const invalid = /^(观众分析|数据概览|直播分析|直播管理|消息|帮助|退出|切换账号)$/
        const candidates = []

        for (const element of document.querySelectorAll('header *, [class*="header"] *, [class*="account"] *, [class*="user"] *')) {
          const text = clean(element.textContent)
          if (text && text.length <= 40 && !invalid.test(text)) {
            const rect = element.getBoundingClientRect()
            if (rect.width > 0 && rect.height > 0 && rect.top >= 0 && rect.top < 120 && rect.right > window.innerWidth * 0.55) {
              candidates.push({ text, score: rect.right + (120 - rect.top) * 10 })
            }
          }
        }

        const lines = (document.body?.innerText || '').split('\n').map(clean).filter(Boolean)
        const analyzeIndex = lines.findIndex((line) => line === '观众分析')
        if (analyzeIndex > 0) {
          const preceding = lines[analyzeIndex - 1]
          if (preceding.length <= 40 && !invalid.test(preceding)) candidates.push({ text: preceding, score: 99999 })
        }

        candidates.sort((a, b) => b.score - a.score)
        const candidate = candidates.find((item, index) =>
          item.text && candidates.findIndex((other) => other.text === item.text) === index
        )?.text
        const title = clean(document.title).replace(/[-_|｜].*$/, '').trim()
        return candidate || title || '当前直播间'
      }
    })
    return String(result || '').trim() || '当前直播间'
  } catch {
    return '当前直播间'
  }
}

function validateBuiltInConfig(value) {
  if (!value || typeof value !== 'object') throw new Error('插件缺少内置连接配置')
  const erpBaseUrl = String(value.erpBaseUrl || '').trim().replace(/\/$/, '')
  const uploadKey = String(value.uploadKey || '').trim()
  const url = new URL(erpBaseUrl)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || !uploadKey) {
    throw new Error('插件内置连接配置无效')
  }
  return Object.freeze({ erpBaseUrl, uploadKey })
}

async function checkBuiltInConnection() {
  connectionVerified = false
  connectionError = ''
  updateConnectionState()
  updateReadButtons()
  updateUploadButton()
  try {
    const response = await fetchErp(settings, PING_PATH, { method: 'GET' })
    assertErpSuccess(response.body, '连接检查失败')
    connectionVerified = true
  } catch (error) {
    connectionError = normalizeErpError(error)
  }
  updateConnectionState()
  updateReadButtons()
  updateUploadButton()
}

async function runOneClick() {
  await fetchRanks(true)
}

async function fetchRanks(autoUpload = false) {
  uploadFailed = false
  if (!activeTab?.id || !pageReady || reading) return
  reading = true
  hideNotice(elements.uploadStatus)
  elements.openErpAfterUpload.classList.add('hidden')
  updateReadButtons()
  updateUploadButton()
  showNotice(elements.readStatus, 'loading', capture
    ? '正在更新榜单，现有结果会保留到读取成功。'
    : '正在读取两个榜单，请稍候。')

  try {
    const latestRoomName = validateRoomName(await readRoomName())
    elements.anchorName.textContent = latestRoomName
    elements.anchorName.title = latestRoomName

    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: activeTab.id, frameIds: [0] },
      world: 'MAIN',
      args: [API_PATH],
      func: async (apiPath) => {
        const controller = new AbortController()
        const timeout = setTimeout(() => controller.abort(), 20000)

        const request = async (rankType, rankDuration) => {
          const query = new URLSearchParams({
            limit: '200',
            tab_key: 'anchor',
            rank_type: rankType,
            rank_duration: rankDuration
          })
          const response = await fetch(`${apiPath}?${query}`, {
            method: 'GET',
            credentials: 'include',
            cache: 'no-store',
            signal: controller.signal,
            headers: {
              Accept: 'application/json, text/plain, */*',
              'Cache-Control': 'no-cache',
              'X-Requested-With': 'XMLHttpRequest',
              'client-id': 'anchor_pc_tinker_proxy',
              'x-appid': '477650',
              'x-sub-web-id': '1116',
              'x-use-bpsc': '1'
            }
          })
          const text = await response.text()
          let body
          try {
            body = JSON.parse(text)
          } catch {
            throw new Error(`${rankType} 接口没有返回 JSON（HTTP ${response.status}）`)
          }
          if (!response.ok) throw new Error(`${rankType} 请求失败（HTTP ${response.status}）`)
          return body
        }

        try {
          const [comment, watch] = await Promise.all([
            request('comment_cnt', '1d'),
            request('watch_duration', 'last_live')
          ])
          return { ok: true, comment, watch }
        } catch (error) {
          return {
            ok: false,
            error: error.name === 'AbortError' ? '读取超时，请稍后重试' : String(error.message || error)
          }
        } finally {
          clearTimeout(timeout)
        }
      }
    })

    if (!result?.ok) throw new Error(result?.error || '榜单读取失败')
    diagnosticRaw = { comment: result.comment, watch: result.watch }
    renderDiagnostics()

    const comment = normalizeRankResponse(result.comment, 'comment')
    const watch = normalizeRankResponse(result.watch, 'watch')
    const capturedAt = Date.now()
    const nextCapture = {
      schemaVersion: 2,
      tabId: activeTab.id,
      sourceUrl: activeTab.url,
      roomName: latestRoomName,
      capturedAt,
      commentDataDate: comment.readyDate,
      watchDataDate: watch.readyDate,
      commentRanks: comment.rows,
      watchRanks: watch.rows,
      mergedCount: countMergedUsers(comment.rows, watch.rows),
      raw: diagnosticRaw,
      uploadState: null
    }

    roomName = latestRoomName
    capture = nextCapture
    renderCapture()
    await persistCapture()
    const safety = getSafetyState()
    if (autoUpload && connectionVerified && !safety.blocking) {
      reading = false
      updateReadButtons()
      updateUploadButton()
      hideNotice(elements.readStatus)
      await uploadCapture()
    } else {
      showNotice(
        elements.readStatus,
        safety.messages.length ? 'error' : 'success',
        safety.messages.length ? '读取结果未通过校验，请按提示重新读取。' : '读取完成，等待系统连接后上传。'
      )
    }
  } catch (error) {
    showNotice(elements.readStatus, 'error', normalizeReadError(error))
    if (capture?.uploadState) renderUploadState()
  } finally {
    reading = false
    updateReadButtons()
    updateUploadButton()
  }
}

function normalizeRankResponse(response, type) {
  const label = type === 'comment' ? '评论榜' : '观看榜'
  if (!response || typeof response !== 'object' || Array.isArray(response)) {
    throw new Error(`${label}响应格式不正确`)
  }

  const code = Number(response.code)
  const subCode = Number(response.subCode ?? response.sub_code ?? 0)
  if (code !== 0 || subCode !== 0) {
    const message = response.message || response.msg || `${code}/${subCode}`
    throw new Error(`${label}接口返回异常：${message}`)
  }

  if (!response.data || typeof response.data !== 'object') throw new Error(`${label}缺少 data`)
  if (!Array.isArray(response.data.ranks)) throw new Error(`${label}缺少 ranks 数组`)
  if (response.data.ranks.length > 200) throw new Error(`${label}返回超过 200 条，已停止处理`)

  const readyDate = String(response.data.ready_date || '').trim()
  if (!isValidDataDate(readyDate)) {
    throw new Error(`${label}数据日期格式不正确：${readyDate || '为空'}`)
  }

  const seen = new Set()
  const seenRanks = new Set()
  const rows = response.data.ranks.map((row, index) => {
    if (!row || typeof row !== 'object' || !row.user || typeof row.user !== 'object') {
      throw new Error(`${label}第 ${index + 1} 条缺少用户信息`)
    }

    const user = row.user
    const secUid = requireText(user.sec_uid, `${label}第 ${index + 1} 条 sec_uid`, 256)
    if (seen.has(secUid)) throw new Error(`${label}存在重复用户：${secUid}`)
    seen.add(secUid)

    const rank = positiveInteger(row.rank, `${label}第 ${index + 1} 条 rank`, 100000)
    if (seenRanks.has(rank)) throw new Error(`${label}存在重复名次：${rank}`)
    seenRanks.add(rank)

    const normalized = {
      rank,
      nickname: requireText(user.nickname, `${label}第 ${index + 1} 条 nickname`, 128),
      secUid,
      isFollower: booleanValue(user.is_follower, `${label}第 ${index + 1} 条 is_follower`),
      isFollowing: booleanValue(user.is_following, `${label}第 ${index + 1} 条 is_following`),
      payLevel: optionalNonNegativeInteger(user.pay_grade?.level, `${label}第 ${index + 1} 条 pay_grade.level`),
      payIconUrl: firstImageUrl(user.pay_grade?.new_im_icon_with_level)
    }

    if (type === 'comment') {
      normalized.commentCount = nonNegativeInteger(row.comment_cnt, `${label}第 ${index + 1} 条 comment_cnt`)
    } else {
      normalized.watchSeconds = nonNegativeInteger(row.watch_time, `${label}第 ${index + 1} 条 watch_time`)
    }
    return normalized
  })

  return { readyDate, rows }
}

function requireText(value, field, maxLength) {
  const text = String(value ?? '').trim()
  if (!text) throw new Error(`${field} 为空`)
  if (maxLength && text.length > maxLength) throw new Error(`${field} 超过 ${maxLength} 个字符`)
  return text
}

function positiveInteger(value, field, maxValue = Number.MAX_SAFE_INTEGER) {
  const number = Number(value)
  if (!Number.isSafeInteger(number) || number < 1 || number > maxValue) throw new Error(`${field} 不是有效正整数`)
  return number
}

function nonNegativeInteger(value, field) {
  const number = Number(value)
  if (!Number.isSafeInteger(number) || number < 0) throw new Error(`${field} 不是有效非负整数`)
  return number
}

function optionalNonNegativeInteger(value, field) {
  if (value === undefined || value === null || value === '') return null
  const number = nonNegativeInteger(value, field)
  if (number > 1000) throw new Error(`${field} 不能超过 1000`)
  return number
}

function booleanValue(value, field) {
  if (value === true || value === 1 || value === '1' || value === 'true') return true
  if (value === false || value === 0 || value === '0' || value === 'false') return false
  throw new Error(`${field} 不是有效布尔值`)
}

function firstImageUrl(image) {
  if (!image || typeof image !== 'object') return null
  const urls = Array.isArray(image.url_list) ? image.url_list : []
  const first = urls.find((value) => typeof value === 'string' && /^https?:\/\//i.test(value.trim()))
  if (!first) return null
  const url = first.trim()
  if (url.length > 1000) throw new Error('等级图标地址超过 1000 个字符')
  return url
}

function isValidDataDate(value) {
  const match = /^(\d{4})([.-])(\d{2})\2(\d{2})$/.exec(String(value || '').trim())
  if (!match) return false
  const year = Number(match[1])
  const month = Number(match[3])
  const day = Number(match[4])
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

function validateRoomName(value) {
  const name = String(value || '').trim()
  if (!name || ['当前直播间', '当前登录账号', '直播服务平台·主播版'].includes(name)) {
    throw new Error('未识别到直播间，请刷新抖音观众分析页后重新打开插件')
  }
  if (name.length > 128) throw new Error('直播间名称异常，请刷新抖音观众分析页后重试')
  return name
}

function countMergedUsers(commentRows, watchRows) {
  return new Set([...commentRows, ...watchRows].map((row) => row.secUid)).size
}

function renderCapture() {
  if (!capture) return
  elements.captureEmpty.classList.add('hidden')
  elements.result.classList.remove('hidden')
  elements.commentDate.textContent = `数据日期 ${capture.commentDataDate}`
  elements.watchDate.textContent = `数据日期 ${capture.watchDataDate}`
  elements.commentCount.textContent = String(capture.commentRanks.length)
  elements.watchCount.textContent = String(capture.watchRanks.length)
  capture.mergedCount = Number.isSafeInteger(capture.mergedCount)
    ? capture.mergedCount
    : countMergedUsers(capture.commentRanks, capture.watchRanks)
  elements.mergedCount.textContent = String(capture.mergedCount)
  elements.capturedAt.textContent = `读取于 ${formatLocalTime(capture.capturedAt)}`
  diagnosticRaw = capture.raw || diagnosticRaw
  renderDiagnostics()
  renderSafetyState()
  renderUploadState()
  updateReadButtons()
  updateUploadButton()
}

function getSafetyState() {
  if (!capture) return { messages: [], blocking: false }
  const messages = []
  let blocking = false
  const commentCount = capture.commentRanks.length
  const watchCount = capture.watchRanks.length
  const age = Date.now() - Number(capture.capturedAt)

  // A completed upload no longer needs freshness or empty-list checks, but date
  // reminders remain visible so operators can still see that Douyin is behind.
  if (!capture.uploadState) {
    if (!Number.isFinite(age) || age < -60000 || age > RESULT_TTL_MS) {
      messages.push('读取结果已超过 30 分钟，请重新读取后再上传。')
      blocking = true
    }

    if (commentCount === 0 && watchCount === 0) {
      messages.push('两个榜单都没有数据，请检查抖音页面后重新读取。')
      blocking = true
    } else if (commentCount === 0 || watchCount === 0) {
      messages.push(commentCount === 0 ? '评论榜为空，请稍后重新读取。' : '观看榜为空，请稍后重新读取。')
      blocking = true
    }
  }

  const expectedYesterday = shanghaiDataDate(-1)
  if (normalizeDataDate(capture.commentDataDate) !== expectedYesterday) {
    messages.push(`评论榜尚未更新：当前 ${capture.commentDataDate || '-'}，应为 ${expectedYesterday}。请12:00以后重新读取。`)
  }

  return { messages, blocking }
}

function renderSafetyState() {
  const safety = getSafetyState()
  elements.safetyMessage.replaceChildren()
  elements.safetyPanel.classList.toggle('hidden', safety.messages.length === 0)
  elements.safetyPanel.classList.toggle('blocking', safety.blocking)
  elements.safetyTitle.textContent = safety.blocking ? '本次数据无法同步' : '数据日期提醒'
  if (safety.messages.length > 0) {
    const list = document.createElement('ul')
    for (const message of safety.messages) {
      const item = document.createElement('li')
      item.textContent = message
      list.append(item)
    }
    elements.safetyMessage.append(list)
  }
}

function renderUploadState() {
  hideNotice(elements.uploadStatus)
  elements.openErpAfterUpload.classList.add('hidden')
  const state = capture?.uploadState
  if (!state) return
  showNotice(elements.uploadStatus, state.type, state.message)
  if (state.needsErp) elements.openErpAfterUpload.classList.remove('hidden')
}

function renderDiagnostics() {
  if (!diagnosticRaw) {
    elements.rawJson.textContent = '尚未读取榜单。'
    elements.diagnosticSummary.textContent = '暂无读取数据'
    return
  }
  elements.rawJson.textContent = JSON.stringify(diagnosticRaw, null, 2)
  elements.diagnosticSummary.textContent = '已有接口数据'
}

async function persistCapture() {
  if (!capture) return
  try {
    await chrome.storage.session.set({ [SESSION_KEY]: capture })
  } catch {
    const withoutRaw = { ...capture, raw: null }
    try {
      await chrome.storage.session.set({ [SESSION_KEY]: withoutRaw })
    } catch {
      // Session recovery is best-effort; the current popup remains usable.
    }
  }
}

async function restoreCapture() {
  try {
    const stored = await chrome.storage.session.get(SESSION_KEY)
    const previous = stored[SESSION_KEY]
    if (!isRestorableCapture(previous)) return
    capture = previous
    capture.uploadState = normalizeStoredUploadState(capture.uploadState)
    uploadFailed = !capture.uploadState && !getSafetyState().blocking
    renderCapture()
    const safety = getSafetyState()
    showNotice(
      elements.readStatus,
      safety.blocking ? 'warning' : 'success',
      safety.blocking ? '已恢复上次结果，但数据已过期，请重新读取。' : '已恢复刚才读取的结果，无需重复读取。'
    )
  } catch {
    // Older Chrome versions may not expose storage.session; reading still works normally.
  }
}

function normalizeStoredUploadState(value) {
  if (!value) return null
  if (value.kind) return value
  if (value.type === 'duplicate') {
    return {
      type: 'warning',
      kind: 'duplicate',
      message: value.message || '这批榜单已经上传过，ERP 未生成重复数据。',
      needsErp: false
    }
  }
  if (value.type === 'success') {
    return {
      type: 'success',
      kind: 'success',
      message: value.message || '上传成功，榜单已经保存到 ERP。',
      needsErp: false
    }
  }
  return value
}

function isRestorableCapture(value) {
  return Boolean(
    value &&
    [1, 2].includes(value.schemaVersion) &&
    value.tabId === activeTab?.id &&
    value.roomName === roomName &&
    Array.isArray(value.commentRanks) &&
    Array.isArray(value.watchRanks)
  )
}

async function uploadCapture() {
  const safety = getSafetyState()
  renderSafetyState()
  if (
    !capture || uploading || reading || capture.uploadState || !settings || safety.blocking
  ) {
    updateUploadButton()
    return
  }

  uploading = true
  uploadFailed = false
  updateUploadButton()
  hideNotice(elements.uploadStatus)
  showNotice(elements.uploadStatus, 'loading', '正在上传，请勿关闭或重复点击。')

  try {
    const payload = {
      roomName: capture.roomName,
      capturedAt: capture.capturedAt,
      commentDataDate: capture.commentDataDate,
      watchDataDate: capture.watchDataDate,
      commentRanks: capture.commentRanks,
      watchRanks: capture.watchRanks
    }
    const response = await fetchErp(settings, IMPORT_PATH, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    const data = assertErpSuccess(response.body, '上传失败')
    capture.uploadState = buildUploadState(data, response.body)
    renderUploadState()
    await persistCapture()
  } catch (error) {
    uploadFailed = true
    showNotice(elements.uploadStatus, 'error', normalizeErpError(error))
  } finally {
    uploading = false
    updateUploadButton()
  }
}

function buildUploadState(data, body) {
  const matchStatus = String(data?.roomMatchStatus ?? body?.roomMatchStatus ?? '').toUpperCase()
  const matchedRoomName = String(data?.matchedRoomName ?? body?.matchedRoomName ?? '').trim()
  const uniqueCount = Number(data?.uniqueUserCount ?? body?.uniqueUserCount ?? capture?.mergedCount ?? 0)
  const newCount = Number(data?.newCustomerCount ?? body?.newCustomerCount ?? 0)
  const updatedCount = Number(data?.updatedCustomerCount ?? body?.updatedCustomerCount ?? 0)
  const customerSummary = `共 ${uniqueCount} 位客户，新增 ${newCount}，更新 ${updatedCount}`
  const duplicate = Boolean(
    data?.duplicate ?? data?.repeated ?? body?.duplicate ?? body?.repeated
  ) || String(data?.status || body?.status || '').toUpperCase() === 'DUPLICATE'
  if (duplicate) {
    const needsErp = matchStatus === 'UNMATCHED' || matchStatus === 'AMBIGUOUS' || !matchStatus
    return {
      type: 'warning',
      kind: 'duplicate',
      message: needsErp
        ? `这批榜单已经上传过，未生成重复数据；${matchStatus === 'AMBIGUOUS' ? '但匹配到多个直播间，请到系统确认归属。' : '但尚未匹配直播间，请到系统处理。'}`
        : `这批榜单已经同步过，无需重复操作${matchedRoomName ? `，已归入“${matchedRoomName}”` : ''}。`,
      needsErp
    }
  }

  if (matchStatus === 'MATCHED') {
    return {
      type: 'success',
      kind: 'success',
      message: matchedRoomName
        ? `同步完成，已归入“${matchedRoomName}”。${customerSummary}。`
        : `同步完成，榜单已匹配直播间。${customerSummary}。`,
      needsErp: false
    }
  }

  if (matchStatus === 'UNMATCHED' || matchStatus === 'AMBIGUOUS') {
    return {
      type: 'warning',
      kind: 'room-warning',
      message: matchStatus === 'AMBIGUOUS'
        ? '榜单已保存，但匹配到多个直播间。请到系统确认归属。'
        : '榜单已保存，但没有匹配到直播间。请到系统处理。',
      needsErp: true
    }
  }

  return {
    type: 'warning',
    kind: 'room-warning',
    message: '榜单已保存，但系统未返回直播间匹配结果。请到系统检查。',
    needsErp: true
  }
}

async function openErp() {
  await chrome.tabs.create({ url: settings.erpBaseUrl })
}

async function fetchErp(connection, path, options) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 20000)
  try {
    const response = await fetch(joinUrl(connection.erpBaseUrl, path), {
      ...options,
      cache: 'no-store',
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        [UPLOAD_HEADER]: connection.uploadKey,
        ...(options.method === 'POST' ? { 'Content-Type': 'application/json' } : {})
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
      const message = body?.msg || body?.message || `HTTP ${response.status}`
      const error = new Error(`ERP 请求失败：${message}`)
      error.httpStatus = response.status
      throw error
    }
    return { body }
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('ERP 连接超时，请检查地址和服务状态')
    throw error
  } finally {
    clearTimeout(timeout)
  }
}

function assertErpSuccess(body, fallback) {
  if (!body || typeof body !== 'object') throw new Error(`${fallback}：ERP 返回内容为空`)
  if (body.code !== undefined && ![0, 200].includes(Number(body.code))) {
    throw new Error(`${fallback}：${body.msg || body.message || `错误码 ${body.code}`}`)
  }
  return body.data && typeof body.data === 'object' ? body.data : body
}

function joinUrl(baseUrl, path) {
  return `${baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
}

function updateConnectionState() {
  elements.connectionState.className = `connection-state${connectionVerified ? ' connected' : ''}`
  elements.connectionState.innerHTML = '<i></i>'
  elements.connectionState.append(document.createTextNode(connectionVerified
    ? '系统连接正常'
    : (connectionError || '正在检查系统')))
  elements.retryConnection.classList.toggle('hidden', !connectionError)
}

function updateReadButtons() {
  elements.fetchRanks.disabled = reading || !pageReady || !connectionVerified
  elements.fetchRanks.textContent = reading
    ? '正在读取榜单…'
    : (!connectionVerified ? (connectionError ? '系统连接不可用' : '正在检查系统') : '读取并同步')
  elements.refreshRanks.disabled = reading || !pageReady || !connectionVerified
  elements.refreshRanks.textContent = reading ? '读取中…' : '重新读取'
}

function updateUploadButton() {
  if (!elements.upload) return
  const state = capture?.uploadState
  const safety = getSafetyState()
  const needsManualUpload = Boolean(capture && !state && uploadFailed && !safety.blocking)
  elements.upload.classList.toggle('hidden', !needsManualUpload)
  if (state) {
    elements.upload.disabled = true
    elements.upload.textContent = state.kind === 'duplicate'
      ? '该批榜单已上传'
      : (state.kind === 'success' ? '上传完成' : '榜单已保存')
    return
  }
  if (reading) {
    elements.upload.disabled = true
    elements.upload.textContent = '正在更新榜单'
    return
  }
  if (!connectionVerified) {
    elements.upload.disabled = true
    elements.upload.textContent = connectionError ? '系统连接不可用' : '正在检查系统'
    return
  }

  if (safety.blocking) {
    elements.upload.disabled = true
    elements.upload.textContent = '请重新读取榜单'
    return
  }
  elements.upload.disabled = !capture || uploading
  elements.upload.textContent = uploading
    ? '正在上传…'
    : `重新同步 ${capture ? `${capture.mergedCount} 位观众` : ''}`
}

async function copyJson() {
  if (!diagnosticRaw) {
    elements.diagnosticSummary.textContent = '请先读取榜单'
    return
  }
  try {
    await navigator.clipboard.writeText(JSON.stringify(diagnosticRaw, null, 2))
    elements.diagnosticSummary.textContent = '已复制技术信息'
  } catch {
    elements.diagnosticSummary.textContent = '复制失败，请导出'
  }
}

function downloadJson() {
  if (!diagnosticRaw) {
    elements.diagnosticSummary.textContent = '请先读取榜单'
    return
  }
  const blob = new Blob([JSON.stringify(diagnosticRaw, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const safeRoomName = (capture?.roomName || roomName || 'unknown').replace(/[^\w\u4e00-\u9fa5-]+/g, '_')
  link.download = `douyin_audience_rank_${safeRoomName}_${Date.now()}.json`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function normalizeDataDate(value) {
  return String(value || '').trim().replace(/-/g, '.')
}

function shanghaiDataDate(dayOffset = 0) {
  const now = new Date(Date.now() + dayOffset * 86400000)
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit'
  }).formatToParts(now)
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  return `${values.year}.${values.month}.${values.day}`
}

function addLocalDays(value, days) {
  const date = new Date(value)
  date.setHours(12, 0, 0, 0)
  date.setDate(date.getDate() + days)
  return date
}

function formatDataDate(value) {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

function formatLocalTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(date)
}

function showNotice(element, type, text) {
  element.className = `notice ${type}`
  element.textContent = text
}

function hideNotice(element) {
  element.className = 'notice hidden'
  element.textContent = ''
}

function normalizeReadError(error) {
  const message = String(error?.message || error || '未知错误')
  if (/没有返回 JSON|Failed to fetch|NetworkError/i.test(message)) {
    return '读取失败，请确认抖音账号仍处于登录状态，再刷新观众分析页重试。'
  }
  if (/Cannot access|permission/i.test(message)) {
    return '插件无法访问当前页面，请刷新抖音页面或重新加载插件后重试。'
  }
  if (/响应格式|缺少|重复用户|重复名次|超过 200|数据日期格式|不是有效|接口返回异常/.test(message)) {
    return `抖音接口数据可能发生变化：${message}。请展开“技术信息”并导出 JSON。`
  }
  return message
}

function normalizeErpError(error) {
  const message = String(error?.message || error || '未知错误')
  if (error?.httpStatus === 401 || error?.httpStatus === 403 || /上传密钥不正确|密钥错误/.test(message)) {
    return '插件版本已过期，请更新插件'
  }
  if (/Failed to fetch|NetworkError/i.test(message)) {
    return '系统连接失败，请确认本地服务已启动'
  }
  if (/Cannot access|permission/i.test(message)) {
    return '插件没有系统访问权限，请重新加载插件'
  }
  return message
}
