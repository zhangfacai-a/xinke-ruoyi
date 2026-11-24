(function (global) {
  'use strict'

  const COMPASS_HOST = 'compass.jinritemai.com'
  const METRICS_PATH = '/screen/live/talent'
  const INVALID_VALUES = new Set(['', '-', '--', '—', '暂无'])
  const METRIC_DEFINITIONS = [
    ['gmv', '直播间成交金额', false],
    ['watch_ucnt', '累计观看人数', false],
    ['follow_anchor_ucnt', '新增粉丝数', false],
    ['pay_ucnt', '成交人数', false],
    ['stat_cost', '千川消耗', false],
    ['live_show_watch_cnt_ratio', '曝光-观看率(次数)', true],
    ['watch_follow_ucnt_ratio', '观看-关注率(人数)', true],
    ['watch_interact_ucnt_ratio', '观看-互动率(人数)', true],
    ['watch_pay_ucnt_ratio', '观看-成交率(人数)', true],
    ['pay_deposit_pre_order_cnt', '预售订单数', false]
  ]
  let lastMissingLabels = []
  const text = value => String(value || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim()
  const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

  function isVisible (element) {
    if (!element?.isConnected) return false
    const style = getComputedStyle(element)
    const rect = element.getBoundingClientRect()
    return style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity || 1) !== 0 && rect.width > 0 && rect.height > 0
  }

  function normalizeRoomName (value) {
    return text(value)
      .replace(/^.*(?:更多操作|网络正常)\s*/i, '')
      .replace(/\s*开播时间[\s\S]*$/i, '')
      .trim()
  }

  function isMetricsRoute () {
    return location.hostname === COMPASS_HOST && location.pathname.startsWith(METRICS_PATH)
  }

  function roomIdFromUrl () {
    const value = new URLSearchParams(location.search).get('live_room_id')
    return /^\d{6,64}$/.test(String(value || '').trim()) ? String(value).trim() : ''
  }

  function normalizeStartTime (value) {
    const match = text(value).match(/(20\d{2})[/.\-](\d{1,2})[/.\-](\d{1,2})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?/)
    if (!match) return ''
    const pad = value => String(value).padStart(2, '0')
    return `${match[1]}-${pad(match[2])}-${pad(match[3])} ${pad(match[4])}:${match[5]}:${match[6] || '00'}`
  }

  function leafTexts (root) {
    return [...root.querySelectorAll('*')]
      .filter(element => element.children.length === 0 && isVisible(element))
      .map(element => text(element.textContent))
      .filter(Boolean)
  }

  function findHeader () {
    const timePattern = /开播时间\s*[:：]?\s*(20\d{2}[/.\-]\d{1,2}[/.\-]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?)(?:\s*开播)?/
    // CSS Modules 的哈希后缀会变化，语义类名前缀保持稳定。
    const headerBlocks = [...document.querySelectorAll('[class*="nameAndTime"]')].filter(isVisible)
    for (const block of headerBlocks) {
      const roomName = normalizeRoomName(block.querySelector('[class*="accountName"]')?.textContent)
      const timeMatch = text(block.querySelector('[class*="startTime"]')?.textContent).match(timePattern)
      if (validRoomName(roomName) && timeMatch) {
        return { roomName, liveStartTime: normalizeStartTime(timeMatch[1]) }
      }
    }

    const semanticTimeElements = [...document.querySelectorAll('[class*="startTime"]')].filter(isVisible)
    for (const timeElement of semanticTimeElements) {
      const timeMatch = text(timeElement.textContent).match(timePattern)
      if (!timeMatch) continue
      const block = timeElement.closest('[class*="nameAndTime"]') || timeElement.parentElement
      const roomName = normalizeRoomName(block?.querySelector?.('[class*="accountName"]')?.textContent)
      if (validRoomName(roomName)) {
        return { roomName, liveStartTime: normalizeStartTime(timeMatch[1]) }
      }
    }

    const timeElements = [...document.querySelectorAll('*')]
      .filter(element => isVisible(element) && timePattern.test(text(element.textContent)))
      .filter(element => ![...element.children].some(child => timePattern.test(text(child.textContent))))
      .sort((left, right) => right.getBoundingClientRect().left - left.getBoundingClientRect().left)
    for (const timeElement of timeElements) {
      const ownText = text(timeElement.textContent)
      const match = ownText.match(timePattern)
      const inlineRoomName = normalizeRoomName(ownText.slice(0, match.index))
      if (validRoomName(inlineRoomName)) {
        return { roomName: inlineRoomName, liveStartTime: normalizeStartTime(match[1]) }
      }
      let container = timeElement.parentElement
      for (let depth = 0; container && depth < 10; depth += 1, container = container.parentElement) {
        const blockText = text(container.textContent)
        const timeIndex = blockText.indexOf('开播时间')
        if (timeIndex > 0) {
          const blockRoomName = normalizeRoomName(blockText.slice(0, timeIndex))
          if (validRoomName(blockRoomName)) {
            return { roomName: blockRoomName, liveStartTime: normalizeStartTime(match[1]) }
          }
        }

        const candidates = [...container.querySelectorAll('*')]
          .filter(element => isVisible(element) && !element.contains(timeElement))
          .map(element => ({
            name: normalizeRoomName(element.textContent),
            area: element.getBoundingClientRect().width * element.getBoundingClientRect().height,
            distance: Math.abs(element.getBoundingClientRect().right - timeElement.getBoundingClientRect().left)
          }))
          .filter(candidate => validRoomName(candidate.name))
          .sort((left, right) => left.distance - right.distance || left.area - right.area || left.name.length - right.name.length)
        if (candidates.length) return { roomName: candidates[0].name, liveStartTime: normalizeStartTime(match[1]) }
      }
    }
    return { roomName: '', liveStartTime: '' }
  }

  function validRoomName (value) {
    const candidate = normalizeRoomName(value)
    if (candidate.length < 2 || candidate.length > 80 || /^\d+$/.test(candidate)) return false
    return /直播间|旗舰店|专卖店|官方店|企业店|店铺/.test(candidate)
  }

  function findRoomName () { return findHeader().roomName }
  function findSession () { return { liveStartTime: findHeader().liveStartTime, liveEndTime: '' } }

  async function waitForHeader (timeoutMs = 30000) {
    const startedAt = Date.now()
    let stableKey = ''
    let stableReads = 0
    while (Date.now() - startedAt < timeoutMs) {
      const header = findHeader()
      const key = `${header.roomName}|${header.liveStartTime}`
      stableReads = header.roomName && header.liveStartTime && key === stableKey
        ? stableReads + 1
        : header.roomName && header.liveStartTime ? 1 : 0
      stableKey = key
      if (stableReads >= 2) return header
      await sleep(500)
    }
    throw new Error('没有识别到右上角直播间名称和开播时间，请确认页面已加载完成')
  }

  function parseMetric (raw, requirePercent = false) {
    const value = text(raw).replace(/[\s¥￥]/g, '')
    if (INVALID_VALUES.has(value)) return null
    const match = value.match(requirePercent ? /^(\d[\d,]*(?:\.\d+)?)%$/ : /^(\d[\d,]*(?:\.\d+)?)(万|亿)?$/)
    if (!match) return null
    return requirePercent
      ? { value: match[1], unit: 'percent' }
      : { value: `${match[1]}${match[2] || ''}`, unit: 'number' }
  }

  function exactLabels (label, root = document) {
    return [...root.querySelectorAll('*')]
      .filter(element => element.children.length === 0 && isVisible(element) && text(element.textContent).replace(/[：:]$/, '') === label)
  }

  function findBlueCardRoot () {
    const labels = METRIC_DEFINITIONS.map(([, label]) => label)
    for (const gmvLabel of exactLabels('直播间成交金额')) {
      let container = gmvLabel.parentElement
      for (let depth = 0; container && depth < 12; depth += 1, container = container.parentElement) {
        if (!isVisible(container)) continue
        const visibleLabels = labels.filter(label => exactLabels(label, container).length > 0)
        if (visibleLabels.length >= 5 && visibleLabels.includes('累计观看人数') && visibleLabels.includes('成交人数')) return container
      }
    }
    return null
  }

  function readMetric (label, requirePercent = false, root = document) {
    const candidates = []
    for (const labelElement of exactLabels(label, root)) {
      const metricCard = labelElement.closest('[class*="container--"]') || labelElement.parentElement?.parentElement?.parentElement
      const directValue = metricCard?.querySelector?.('[class*="value--"]')
      if (directValue && isVisible(directValue)) {
        const metric = parseMetric(directValue.textContent, requirePercent)
        if (metric) return metric
      }
      let container = labelElement.parentElement
      for (let depth = 0; container && depth < 5; depth += 1, container = container.parentElement) {
        const values = leafTexts(container).filter(value => value !== label).map(value => parseMetric(value, requirePercent)).filter(Boolean)
        if (values.length >= 1 && values.length <= 3) {
          const rect = container.getBoundingClientRect()
          candidates.push({ metric: values[0], area: rect.width * rect.height, top: rect.top, depth })
          break
        }
      }
    }
    candidates.sort((left, right) => left.area - right.area || left.depth - right.depth || left.top - right.top)
    return candidates[0]?.metric || null
  }

  function readOdometer (inside) {
    if (!inside) return ''
    return [...inside.children].map(part => {
      if (part.classList.contains('odometer-formatting-mark')) return text(part.textContent)
      if (!part.classList.contains('odometer-digit')) return ''
      const values = [...part.querySelectorAll('.odometer-value')]
      const current = part.querySelector('.odometer-last-value') || values.at(-1)
      return text(current?.textContent)
    }).join('')
  }

  function readGmv (root = document) {
    for (const labelElement of exactLabels('直播间成交金额', root)) {
      const card = labelElement.closest('[class*="gmv--"]') || labelElement.parentElement?.parentElement?.parentElement
      if (!card || !isVisible(card)) continue
      const odometer = card.querySelector('.odometer-inside')
      if (odometer) {
        const parts = readOdometer(odometer)
        const parsed = parseMetric(parts)
        if (parsed) return parsed
      }
    }
    return readMetric('直播间成交金额', false, root)
  }

  function numericValue (metric) {
    const match = String(metric?.value || '').replace(/,/g, '').match(/^(\d+(?:\.\d+)?)(万|亿)?$/)
    if (!match) return null
    return Number(match[1]) * (match[2] === '亿' ? 100000000 : match[2] === '万' ? 10000 : 1)
  }

  function readRequiredBlueCard () {
    const root = findBlueCardRoot()
    if (!root) {
      lastMissingLabels = METRIC_DEFINITIONS.map(([, label]) => label)
      return null
    }
    const result = {}
    for (const [key, label, percent] of METRIC_DEFINITIONS) result[key] = key === 'gmv' ? readGmv(root) : readMetric(label, percent, root)
    lastMissingLabels = METRIC_DEFINITIONS.filter(([key]) => !result[key]).map(([, label]) => label)
    return lastMissingLabels.length ? null : withDerivedMetrics(result)
  }

  function withDerivedMetrics (metrics) {
    const watch = numericValue(metrics.watch_ucnt)
    const interactionRate = Number(metrics.watch_interact_ucnt_ratio?.value)
    return { ...metrics, content_interaction_cnt: { value: String(Math.round(watch * interactionRate / 100)), unit: 'number' } }
  }

  async function waitUntilReady (reader, timeoutMs = 15000) {
    const startedAt = Date.now()
    let previous = ''
    let stableReads = 0
    while (Date.now() - startedAt < timeoutMs) {
      const value = await reader()
      const signature = value ? JSON.stringify(value) : ''
      stableReads = signature && signature === previous ? stableReads + 1 : signature ? 1 : 0
      previous = signature
      if (stableReads >= 3) return value
      await sleep(350)
    }
    if (lastMissingLabels.length) throw new Error(`蓝色卡片缺少：${lastMissingLabels.join('、')}。请在蓝色卡片的指标设置中勾选这些字段后重试`)
    throw new Error('蓝色数据卡尚未加载完整，请等待页面加载后重试')
  }

  async function collect ({ onProgress } = {}) {
    if (!isMetricsRoute()) throw new Error('请打开巨量百应“直播大屏·专业版”页面')
    const platformSessionId = roomIdFromUrl()
    if (!platformSessionId) throw new Error('地址栏缺少有效的 live_room_id，无法识别直播场次')
    const header = findHeader()
    if (!header.roomName) throw new Error('没有识别到右上角直播间名称，请等待页面加载完成')
    if (!header.liveStartTime) throw new Error('没有识别到右上角开播时间，请等待页面加载完成')
    lastMissingLabels = []
    onProgress?.('正在读取蓝色数据卡')
    const metrics = await waitUntilReady(readRequiredBlueCard)
    const verifiedHeader = findHeader()
    if (roomIdFromUrl() !== platformSessionId) throw new Error('读取过程中直播场次发生切换，请重新读取')
    if (normalizeRoomName(verifiedHeader.roomName) !== normalizeRoomName(header.roomName) || verifiedHeader.liveStartTime !== header.liveStartTime) {
      throw new Error('读取过程中直播间信息发生变化，请等待页面稳定后重试')
    }
    return {
      roomName: header.roomName,
      platformSessionId,
      liveStartTime: header.liveStartTime,
      liveEndTime: '',
      pageUrl: location.href,
      capturedAt: Date.now(),
      metrics
    }
  }

  function hasSelectedSession () { return isMetricsRoute() && Boolean(roomIdFromUrl()) }

  global.XinkeJrmDomCollector = Object.freeze({
    collect, findRoomName, findSession, waitForHeader, roomIdFromUrl, hasSelectedSession,
    isMetricsRoute, normalizeRoomName, readOdometer,
    isSessionSelectionPage: () => isMetricsRoute() && !hasSelectedSession()
  })
})(globalThis)
