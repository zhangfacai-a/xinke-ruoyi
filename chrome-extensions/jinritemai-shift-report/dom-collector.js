(function (global) {
  'use strict'

  const COMPASS_HOST = 'compass.jinritemai.com'
  const METRICS_PATHS = ['/talent/live-detail', '/talent/live-statement']
  const INVALID_VALUES = new Set(['', '-', '--', '—', '暂无'])

  const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
  const text = value => String(value || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim()

  function normalizeRoomName (value) {
    return text(value)
      .replace(/\s*(?:(?:直播)?开播时间|直播开始时间)\s*[:：]?\s*20\d{2}[/.\-]\d{1,2}[/.\-]\d{1,2}[\s\S]*$/i, '')
      .replace(/\s*已直播\s*\d+\s*(?:天|小时|时|分钟|分|秒)[\s\S]*$/i, '')
      .trim()
  }

  function isVisible (element) {
    if (!element?.isConnected) return false
    const style = getComputedStyle(element)
    const rect = element.getBoundingClientRect()
    return style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity || 1) !== 0 && rect.width > 0 && rect.height > 0
  }

  function exactTextElements (label, root = document) {
    return [...root.querySelectorAll('*')].filter(element => isVisible(element) && text(element.textContent) === label)
  }

  function validRoomName (value) {
    const candidate = normalizeRoomName(value)
    if (candidate.length < 2 || candidate.length > 128 || /^\d+$/.test(candidate)) return ''
    if (/^(直播间|直播数据|直播大屏|巨量百应|全部直播间|请选择直播间|个人中心|帮助|退出|登录)$/.test(candidate)) return ''
    return candidate
  }

  function findRoomName () {
    const preferredSelectors = [
      '[class*="userCenter-"] [class*="userName-"]',
      '[class*="userDropDown-"] [class*="userName-"]',
      '[class*="userDropDown" i] [class*="userName" i]',
      '[class*="user-dropdown" i] [class*="user-name" i]',
      '[class*="accountDropDown" i] [class*="accountName" i]'
    ]
    for (const selector of preferredSelectors) {
      const element = document.querySelector(selector)
      const candidate = validRoomName(element?.getAttribute?.('title')) || validRoomName(element?.textContent)
      if (candidate) return candidate
    }

    const candidates = [...new Set(document.querySelectorAll(
      'header *,[class*="header" i] *,[class*="account" i] *,[class*="shop" i] *,[class*="user" i] *,[class*="nick" i],[title]'
    ))]
    let best = null
    for (const element of candidates.slice(0, 2500)) {
      if (!isVisible(element)) continue
      const rect = element.getBoundingClientRect()
      if (rect.top < 0 || rect.top > 130 || rect.left < window.innerWidth * 0.52) continue
      const titled = validRoomName(element.getAttribute?.('title'))
      const candidate = titled || validRoomName(element.textContent)
      if (!candidate || candidate.length > 50) continue
      if (/首页|短视频|图文|橱窗|交易|商品|商家|人群|市场|搜索|数据|分析|成交|曝光|互动|排行|趋势|选择|概览|大屏|诊断|指标/.test(candidate)) continue

      const marker = `${element.className || ''} ${element.parentElement?.className || ''}`
      const context = element.parentElement?.parentElement || element.parentElement || element
      let score = 10
      if (titled) score += 4
      if (/直播间/.test(candidate)) score += 10
      if (/旗舰店|专卖店|官方店|店铺/.test(candidate)) score += 8
      if (/shop|account|user|nick|avatar|name/i.test(marker)) score += 6
      if (context.querySelector?.('img,[class*="avatar" i]')) score += 6
      if (rect.left > window.innerWidth * 0.72) score += 4
      if (element.children.length === 0) score += 2
      if (!best || score > best.score) best = { name: candidate, score }
    }
    return best?.score >= 20 ? best.name : ''
  }

  function normalizeStartTime (value) {
    const match = text(value).match(/(20\d{2})[/.\-](\d{1,2})[/.\-](\d{1,2})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?/)
    if (!match) return ''
    const pad = number => String(number).padStart(2, '0')
    return `${match[1]}-${pad(match[2])}-${pad(match[3])} ${pad(match[4])}:${match[5]}:${match[6] || '00'}`
  }

  function findSession () {
    const bodyText = text(document.body?.innerText)
    const start = bodyText.match(/直播开始时间\s*[:：]?\s*(20\d{2}[/.\-]\d{1,2}[/.\-]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?)/)
    return {
      liveStartTime: normalizeStartTime(start?.[1])
    }
  }

  function isMetricsRoute () {
    return location.hostname === COMPASS_HOST && METRICS_PATHS.some(path => location.pathname.startsWith(path))
  }

  function roomIdFromUrl () {
    if (location.pathname.startsWith('/talent/live-statement')) {
      const roomId = new URLSearchParams(location.search).get('live_room_id')
      return /^\d{6,}$/.test(String(roomId || '').trim()) ? String(roomId).trim() : ''
    }
    return ''
  }

  function hasSelectedSession () {
    if (!isMetricsRoute()) return false
    return Boolean(
      roomIdFromUrl() ||
      document.querySelector('[data-kora-exposure*="直播间详情-基本信息"]') ||
      findSession().liveStartTime
    )
  }

  function metricValue (raw) {
    const value = text(raw).replace(/\s+/g, '')
    if (INVALID_VALUES.has(value)) return null
    return /^\d[\d,]*(?:\.\d+)?(?:万|亿)?$/.test(value) ? { value, unit: 'number' } : null
  }

  function groupCard (name) {
    return [...document.querySelectorAll('[class*="coreIndexItemWrapper-"]')]
      .find(card => isVisible(card) && text(card.querySelector('[class*="groupName-"]')?.textContent) === name)
  }

  function isGroupActive (card) {
    return [...(card?.classList || [])].some(className => className.startsWith('active-'))
  }

  async function selectGroup (name, timeoutMs = 12000) {
    const card = groupCard(name)
    if (!card) throw new Error(`没有找到“${name}”数据分组，请确认页面已加载完成`)
    if (!isGroupActive(card)) card.click()

    const startedAt = Date.now()
    while (Date.now() - startedAt < timeoutMs) {
      const current = groupCard(name)
      if (current && isGroupActive(current)) return
      await sleep(200)
    }
    throw new Error(`“${name}”数据页加载超时，请稍后重试`)
  }

  function funnelSection (title) {
    const headings = exactTextElements(title)
    const heading = headings.find(element => String(element.className || '').includes('title-')) || headings[0]
    let section = heading?.parentElement || null
    for (let depth = 0; section && depth < 5; depth += 1, section = section.parentElement) {
      if (section.querySelector('[class*="funnel-small-wrapper"]')) return section
    }
    return null
  }

  function readFunnelMetric (funnelTitle, metricLabel) {
    const section = funnelSection(funnelTitle)
    if (!section) return null
    const wrapper = [...section.querySelectorAll('[class*="funnel-small-wrapper"]')]
      .find(element => {
        const label = element.querySelector('[class*="funnel-small-label"]')
        return text(label?.textContent) === metricLabel
      })
    const valueElement = wrapper?.querySelector('[class*="funnel-small-num"]')
    return metricValue(valueElement?.textContent)
  }

  function readFunnels () {
    const metrics = {
      live_show_cnt: readFunnelMetric('成交转化漏斗', '直播间曝光人数'),
      watch_ucnt: readFunnelMetric('成交转化漏斗', '直播间观看人数'),
      pay_ucnt: readFunnelMetric('成交转化漏斗', '成交人数'),
      content_interaction_cnt: readFunnelMetric('互动转化漏斗', '内容互动人数')
    }
    return Object.values(metrics).every(Boolean) ? metrics : null
  }

  function readFans () {
    const titleCandidates = exactTextElements('新增粉丝数')
    const title = titleCandidates.find(element => String(element.className || '').includes('cardTitle-'))
      || titleCandidates.find(element => element.closest('[class*="card-"]')?.querySelector('[class*="valueText-"]'))
    const card = title?.closest('[class*="card-"]') || title?.parentElement
    const valueElement = card?.querySelector('[class*="valueText-"]')
    if (!valueElement) return null

    const values = [...valueElement.querySelectorAll('span')]
      .filter(isVisible)
      .map(element => metricValue(element.textContent))
      .filter(Boolean)
    return values[values.length - 1] || metricValue(valueElement.textContent)
  }

  async function waitUntilReady (reader, errorMessage, timeoutMs = 15000) {
    const startedAt = Date.now()
    let firstValidAt = 0
    let latest = null
    let previous = ''
    let stableReads = 0
    while (Date.now() - startedAt < timeoutMs) {
      const value = reader()
      const signature = value ? JSON.stringify(value) : ''
      if (signature) {
        latest = value
        if (!firstValidAt) firstValidAt = Date.now()
        stableReads = signature === previous ? stableReads + 1 : 1
      } else {
        stableReads = 0
      }
      previous = signature
      if (stableReads >= 2) return value

      // 正在直播时人数会持续变化，完整数据连续两秒可读即可采用最新值，
      // 不再要求所有指标长时间完全静止。
      if (latest && Date.now() - firstValidAt >= 2000) return latest
      await sleep(300)
    }
    throw new Error(errorMessage)
  }

  async function collect ({ onProgress } = {}) {
    if (!isMetricsRoute()) throw new Error('请先打开巨量百应直播数据页面')
    const session = findSession()
    if (!session.liveStartTime) throw new Error('当前尚未选择具体直播场次')
    const roomName = findRoomName()
    if (!roomName) throw new Error('没有识别到右上角直播间名称，请等待页面加载完成')

    const originalGroup = [...document.querySelectorAll('[class*="coreIndexItemWrapper-"]')]
      .find(isGroupActive)
    const originalGroupName = text(originalGroup?.querySelector('[class*="groupName-"]')?.textContent)

    let funnelMetrics
    let fanMetric
    try {
      onProgress?.('正在读取成交和互动漏斗')
      await selectGroup('流量/转化')
      funnelMetrics = await waitUntilReady(readFunnels, '漏斗数据尚未加载完整，请等待页面加载后重试')

      onProgress?.('正在读取新增粉丝数')
      await selectGroup('互动')
      fanMetric = await waitUntilReady(readFans, '新增粉丝数尚未加载完成，请稍后重试')
    } finally {
      if (originalGroupName && originalGroupName !== '互动') {
        await selectGroup(originalGroupName, 5000).catch(() => {})
      }
    }

    return {
      roomName,
      liveStartTime: session.liveStartTime,
      pageUrl: location.href,
      capturedAt: Date.now(),
      metrics: { ...funnelMetrics, follow_anchor_ucnt: fanMetric }
    }
  }

  global.XinkeJrmDomCollector = Object.freeze({
    collect,
    findRoomName,
    findSession,
    hasSelectedSession,
    isMetricsRoute,
    normalizeRoomName,
    isSessionSelectionPage: () => isMetricsRoute() && !hasSelectedSession()
  })
})(globalThis)
