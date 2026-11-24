const COLLECTOR = globalThis.XinkeJrmDomCollector
const UI_STATE = globalThis.XinkeJrmUiState
const selectedKey = roomId => `jrm_anchor_${roomId}`
const isTarget = () => location.hostname === 'compass.jinritemai.com' && location.pathname.startsWith('/screen/live/talent') && COLLECTOR?.hasSelectedSession?.()
const send = async message => { const result = await chrome.runtime.sendMessage(message); if (!result?.ok) throw new Error(result?.error || '操作失败'); return result.data }
const expandNumber = metric => {
  const value = String(metric?.value ?? '').replace(/[,￥¥\s]/g, '')
  if (!value || ['-', '--', '—', '暂无'].includes(value)) return 0
  const match = value.match(/^([0-9]+(?:\.[0-9]+)?)(万|亿)?$/)
  if (!match) throw new Error(`无法识别数据“${value}”`)
  return Number(match[1]) * (match[2] === '亿' ? 100000000 : match[2] === '万' ? 10000 : 1)
}
const percent = metric => {
  const value = Number(String(metric?.value ?? '0').replace('%', ''))
  if (!Number.isFinite(value) || value < 0 || value > 100) throw new Error('比例数据格式异常')
  return Number(value.toFixed(4))
}
const totals = record => {
  const metrics = record.metrics || {}
  const missing = UI_STATE.missingBlueCardMetrics(metrics)
  if (missing.length) throw new Error(`蓝色卡片缺少：${missing.join('、')}。请在指标设置中勾选后重试`)
  const value = {
    capturedAt: record.capturedAt,
    watchTotal: Math.round(expandNumber(metrics.watch_ucnt)), interactionTotal: Math.round(expandNumber(metrics.content_interaction_cnt)),
    newFanTotal: Math.round(expandNumber(metrics.follow_anchor_ucnt)), dealTotal: Math.round(expandNumber(metrics.pay_ucnt)),
    gmvTotal: Number(expandNumber(metrics.gmv).toFixed(4)), adCostTotal: Number(expandNumber(metrics.stat_cost).toFixed(4)),
    presaleOrderTotal: Math.round(expandNumber(metrics.pay_deposit_pre_order_cnt)), exposureWatchRate: percent(metrics.live_show_watch_cnt_ratio),
    watchFollowRate: percent(metrics.watch_follow_ucnt_ratio), watchInteractionRate: percent(metrics.watch_interact_ucnt_ratio), watchDealRate: percent(metrics.watch_pay_ucnt_ratio)
  }
  if (!Number.isFinite(value.gmvTotal) || value.gmvTotal > 99999999999999.9999) throw new Error('成交金额识别异常，请等待数字停止滚动后重试')
  if (!Number.isFinite(value.adCostTotal) || value.adCostTotal > 99999999999999.9999) throw new Error('千川消耗识别异常，请等待页面稳定后重试')
  return value
}
const formatTime = value => {
  if (!value) return '-'
  if (typeof value === 'number') return new Date(value).toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-').slice(0, 16)
  return String(value).replace('T', ' ').slice(0, 16)
}
const formatRange = (start, end) => {
  const [startDate = '', startTime = ''] = formatTime(start).split(' ')
  const [endDate = '', endTime = ''] = formatTime(end).split(' ')
  return startDate === endDate
    ? `${startDate.slice(5)} ${startTime} - ${endTime}`
    : `${startDate.slice(5)} ${startTime} - ${endDate.slice(5)} ${endTime}`
}
const number = value => Number(value || 0).toLocaleString('zh-CN')
const money = value => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function createUi () {
  if (!isTarget() || document.getElementById('xinke-handover-host')) return null
  const mountedSessionId = COLLECTOR.roomIdFromUrl()
  const host = document.createElement('div')
  host.id = 'xinke-handover-host'
  host.attachShadow({ mode: 'open' })
  host.shadowRoot.innerHTML = `<style>
    :host{all:initial}.panel{position:fixed;z-index:2147483647;right:18px;top:68px;width:min(500px,calc(100vw - 36px));max-height:calc(100vh - 84px);overflow:hidden;background:#fff;border:1px solid #d8dee8;border-top:3px solid #f26b21;border-radius:6px;box-shadow:0 10px 28px rgba(24,36,51,.16);font:13px/1.45 Arial,"Microsoft YaHei",sans-serif;color:#182230;letter-spacing:0}.head{height:42px;display:flex;align-items:center;justify-content:space-between;padding:0 12px;border-bottom:1px solid #edf0f4;cursor:move;user-select:none;touch-action:none}.head:active{cursor:grabbing}.title{font-size:15px;font-weight:700}.connection{margin-left:7px;color:#147d59;font-size:10px;font-weight:400}.connection:before{content:'';display:inline-block;width:6px;height:6px;border-radius:50%;background:#18a46b;margin-right:4px}.connection.checking{color:#ad6800}.connection.checking:before{background:#faad14}.connection.error{color:#b42318}.connection.error:before{background:#d92d20}.icon{width:28px;height:28px;border:0;background:transparent;color:#667085;cursor:pointer;font-size:16px}.body{box-sizing:border-box;max-height:calc(100vh - 129px);overflow:auto;padding:10px 12px 11px}.room{font-size:14px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.session{margin-top:1px;color:#7b8494;font-size:10px}.last{display:flex;align-items:center;gap:7px;margin:8px 0;color:#667085;font-size:11px;min-width:0}.last span{flex:none}.last b{min-width:0;color:#344054;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.label{display:block;margin:8px 0 4px;color:#667085;font-size:11px}.select{box-sizing:border-box;width:100%;height:36px;border:1px solid #cbd2dc;border-radius:5px;padding:0 9px;background:#fff;color:#182230}.select:focus{border-color:#f26b21;outline:3px solid rgba(242,107,33,.12)}.button{width:100%;height:38px;border-radius:5px;border:0;background:#f26b21;color:#fff;font-size:13px;font-weight:700;cursor:pointer;margin-top:8px}.button:hover:not(:disabled){background:#df5513}.button:active:not(:disabled){background:#c9470c}.button:disabled{background:#d9dee7;color:#8993a2;cursor:not-allowed}.select:disabled{background:#f4f6f8;color:#8993a2;cursor:not-allowed}.report{display:none;margin-top:8px}.report.show{display:block}.review-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:6px}.review-head small{display:block;color:#7b8494;font-size:10px}.review-head strong{display:block;font-size:13px}.range{display:flex;align-items:center;justify-content:space-between;padding:6px 8px;background:#fff5ef;border:1px solid #fde1d2;border-radius:5px;font-size:10px}.range b{color:#d85209}.grid{display:grid;grid-template-columns:minmax(68px,.8fr) minmax(0,1fr) minmax(0,1fr);margin-top:6px;border:1px solid #e4e8ee;border-radius:5px;overflow:hidden;font-variant-numeric:tabular-nums}.grid>div{min-width:0;padding:4px 6px;border-bottom:1px solid #edf0f3;overflow:hidden;text-align:right;text-overflow:ellipsis;white-space:nowrap}.grid>div:nth-child(3n+1){text-align:left;color:#596273}.grid .header{background:#f7f8fa;color:#7b8494;font-size:10px}.grid .delta{font-weight:700;color:#e65f19}.grid .delta.adjusted{color:#b54708}.grid>div:nth-last-child(-n+3){border-bottom:0}.confirm{position:sticky;bottom:-11px;z-index:3;margin-top:7px;background:#f26b21;box-shadow:0 -7px 12px 7px #fff}.confirm:hover:not(:disabled){background:#df5513}.feedback{display:none;margin-top:7px;color:#667085;font-size:11px}.feedback.show{display:block}.feedback.error,.feedback.warn{padding:7px 8px;border-radius:5px}.feedback.error{background:#fff1f0;color:#b42318}.feedback.warn{background:#fff7e6;color:#ad6800}.success{display:none;padding:12px 0 1px;text-align:center}.success.show{display:block}.success-mark{display:grid;width:38px;height:38px;margin:0 auto 7px;place-items:center;border-radius:50%;background:#e7f7f0;color:#14845c;font-size:21px;font-weight:700}.success h3{margin:0;font-size:16px}.success>p{margin:3px 0 10px;color:#667085;font-size:11px}.success-summary{padding:8px 9px;border:1px solid #dcebe5;border-radius:5px;background:#f5fbf8;text-align:left}.success-summary span{display:block;color:#667085;font-size:10px}.success-summary strong{display:block;margin-top:3px;color:#185c46;font-size:12px}.success-metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:5px;margin-top:6px}.success-metrics div{min-width:0;padding:5px 3px;border-radius:4px;background:#fff;overflow:hidden}.success-metrics small,.success-metrics b{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.success-metrics small{color:#7b8494}.success-metrics b{margin-top:1px;color:#182230}.next{margin-top:10px}.foot{display:flex;justify-content:space-between;margin-top:8px;padding-top:7px;border-top:1px solid #edf0f4}.link{border:0;background:transparent;color:#d85209;cursor:pointer;padding:2px 0;font-size:11px}.link:hover{color:#b94308}.collapsed{width:190px}.collapsed .body{display:none}.hidden{display:none!important}
  </style><aside class="panel"><header class="head"><div class="title">主播上报 <span class="connection checking" id="connection">连接中</span></div><button class="icon" id="fold" title="收起">−</button></header><main class="body"><div class="room" id="room">正在识别直播间</div><div class="session" id="session"></div><div id="workflow"><div id="entry"><div class="last"><span>上次</span><b id="last">正在读取</b></div><label class="label" for="anchor">本次主播</label><select class="select" id="anchor"><option value="">请选择主播</option></select><button class="button" id="read" disabled>读取并核对</button></div><section class="report" id="report"><div class="review-head"><div><small>本次主播</small><strong id="review-anchor"></strong></div><button class="link" id="reselect">重新选择</button></div><div class="range"><span id="range"></span><b id="mode"></b></div><div class="grid" id="metrics"></div><button class="button confirm" id="submit">确认上报给该主播</button></section><div class="feedback" id="feedback"></div></div><section class="success" id="success"><div class="success-mark">✓</div><h3 id="success-title">上报完成</h3><p>数据已归属，不会重复计算</p><div class="success-summary"><span id="success-range"></span><strong id="success-mode"></strong><div class="success-metrics" id="success-metrics"></div></div><button class="button next" id="next">继续上报</button></section><div class="foot"><button class="link" id="refresh">重新同步</button><button class="link" id="open">选择场次 ↗</button></div></main></aside>`
  document.documentElement.appendChild(host)
  const root = host.shadowRoot
  const $ = id => root.getElementById(id)
  const compactStyle = document.createElement('style')
  compactStyle.textContent = '.grid{grid-template-columns:repeat(3,minmax(0,1fr));gap:6px;padding:0;border:0;background:transparent}.grid>div{min-width:0;padding:7px 9px!important;border:1px solid #e7eaf0!important;border-radius:5px;background:#fff;text-align:left!important}.metric-label{display:block;overflow:hidden;color:#344054;font-size:11px;font-weight:600;white-space:nowrap;text-overflow:ellipsis}.metric-values{display:flex;flex-direction:column;align-items:flex-start;gap:1px;margin-top:4px}.metric-current{min-width:0;max-width:100%;overflow:hidden;color:#8993a2;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.metric-result{order:-1;max-width:100%;overflow:hidden;color:#e65f19;font-size:14px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.metric-result.adjusted{color:#b54708}.range{background:#fff7f2;border-color:#fbd8c5}.range b{font-size:10px}.confirm{height:42px;font-size:14px}.foot{margin-top:7px;padding-top:6px}@media(max-width:720px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}'
  root.append(compactStyle)
  root.querySelector('.title').firstChild.textContent = '本次数据确认 '
  $('refresh').textContent = '刷新数据'
  $('open').textContent = '切换场次'
  const panel = root.querySelector('.panel')
  const head = root.querySelector('.head')
  let drag = null
  head.addEventListener('pointerdown', event => {
    if (event.button !== 0 || event.target.closest('button')) return
    const rect = panel.getBoundingClientRect()
    drag = { pointerId: event.pointerId, offsetX: event.clientX - rect.left, offsetY: event.clientY - rect.top }
    head.setPointerCapture(event.pointerId)
  })
  head.addEventListener('pointermove', event => {
    if (!drag || drag.pointerId !== event.pointerId) return
    const maxLeft = Math.max(8, window.innerWidth - panel.offsetWidth - 8)
    const maxTop = Math.max(8, window.innerHeight - panel.offsetHeight - 8)
    panel.style.left = `${Math.min(maxLeft, Math.max(8, event.clientX - drag.offsetX))}px`
    panel.style.top = `${Math.min(maxTop, Math.max(8, event.clientY - drag.offsetY))}px`
    panel.style.right = 'auto'
  })
  const stopDrag = event => {
    if (!drag || drag.pointerId !== event.pointerId) return
    drag = null
    if (head.hasPointerCapture(event.pointerId)) head.releasePointerCapture(event.pointerId)
  }
  head.addEventListener('pointerup', stopDrag)
  head.addEventListener('pointercancel', stopDrag)
  const state = { context: null, record: null, preview: null, success: null, phase: 'idle', busy: false, destroyed: false, retryTimer: null }
  const selectedId = () => Number($('anchor').value || 0)
  const selectedName = () => $('anchor').selectedOptions[0]?.textContent || ''
  const feedback = (text = '', type = '') => { $('feedback').textContent = text; $('feedback').className = `feedback ${text ? 'show' : ''} ${type}` }
  const connection = (status, text) => { $('connection').className = `connection ${status}`; $('connection').textContent = text }
  const renderBusy = () => { $('anchor').disabled = state.busy; $('read').disabled = state.busy || !selectedId() || !state.context?.roomMatched; $('submit').disabled = state.busy; $('next').disabled = state.busy }
  const contextRequest = async () => {
    try { const value = await send({ type: 'JRM_CONTEXT', platformSessionId: state.record.platformSessionId, roomName: state.record.roomName }); connection('', '已连接'); return value }
    catch (error) { connection('error', '连接异常'); throw error }
  }
  const clearPreview = () => { state.preview = null; $('entry').classList.remove('hidden'); $('report').classList.remove('show'); $('read').textContent = '读取并核对' }
  const showWorkflow = () => { state.phase = 'idle'; $('workflow').classList.remove('hidden'); $('success').classList.remove('show') }
  const populateAnchors = async () => {
    const saved = state.context.roomId ? (await chrome.storage.local.get(selectedKey(state.context.roomId)))[selectedKey(state.context.roomId)] : null
    $('anchor').replaceChildren(new Option('请选择主播', ''))
    for (const item of state.context.anchors || []) $('anchor').append(new Option(item.userName, String(item.userId)))
    if (saved && [...$('anchor').options].some(option => Number(option.value) === Number(saved))) $('anchor').value = String(saved)
  }
  const renderContext = () => {
    showWorkflow()
    $('room').textContent = state.record.roomName
    $('session').textContent = `开播 ${formatTime(state.record.liveStartTime)} · 场次 ${state.record.platformSessionId.slice(-8)}`
    const last = state.context.lastReport
    $('last').textContent = last ? `${last.anchorName} · ${formatTime(last.endTime)} · 观看 ${number(last.watchDelta)} · ¥${money(last.gmvDelta)}` : '暂无 · 从开播时间起算'
    if (!state.context.roomMatched) feedback('该直播间尚未绑定，请先在网站配置。', 'error')
    else if (!(state.context.anchors || []).length) feedback('该直播间没有配置主播。', 'error')
    else feedback('')
    renderBusy()
  }
  const load = async () => {
    if (state.busy || state.destroyed) return
    clearTimeout(state.retryTimer)
    state.busy = true; connection('checking', '连接中'); showWorkflow(); clearPreview(); feedback('正在同步本场上报记录…'); renderBusy()
    try {
      const header = await COLLECTOR.waitForHeader(30000)
      const platformSessionId = COLLECTOR.roomIdFromUrl()
      if (platformSessionId !== mountedSessionId || state.destroyed) return
      state.record = { roomName: header.roomName, platformSessionId, liveStartTime: header.liveStartTime }
      state.context = await contextRequest()
      await populateAnchors()
      renderContext()
    } catch (error) {
      feedback(`${error?.message || '读取失败'}，3 秒后重试。`, 'error')
      state.retryTimer = setTimeout(load, 3000)
    } finally { state.busy = false; renderBusy() }
  }
  const renderPreview = value => {
    $('entry').classList.add('hidden')
    $('review-anchor').textContent = value.anchorName
    $('range').textContent = `统计时段 ${formatRange(value.startTime, value.endTime)}`
    $('mode').textContent = value.firstReport ? '从开播开始' : '接上次上报'
    const definitions = [
      ['成交金额', '直播间成交金额', 'gmvTotal', 'money'], ['观看人数', '累计观看人数', 'watchTotal', 'number'],
      ['新增粉丝', '新增粉丝数', 'newFanTotal', 'number'], ['成交人数', '成交人数', 'dealTotal', 'number'],
      ['投流消耗', '千川消耗', 'adCostTotal', 'money'], ['进房率', '曝光-观看率(次数)', 'exposureWatchRate', 'rate'],
      ['转粉率', '观看-关注率(人数)', 'watchFollowRate', 'rate'], ['互动率', '观看-互动率(人数)', 'watchInteractionRate', 'rate'],
      ['成交率', '观看-成交率(人数)', 'watchDealRate', 'rate'], ['预售订单', '预售订单数', 'presaleOrderTotal', 'number']
    ]
    $('metrics').replaceChildren()
    for (const [label, originalLabel, key, type] of definitions) {
      const currentText = type === 'money' ? `¥${money(value.current[key])}` : type === 'rate' ? `${Number(value.current[key] || 0).toFixed(2)}%` : number(value.current[key])
      const change = type === 'rate' ? value.intervalRate[key] : value.delta[key]
      const changeText = type === 'money' ? `¥${money(change)}` : type === 'rate' ? (change == null ? '无法计算' : `${Number(change).toFixed(2)}%`) : number(change)
      const item = document.createElement('div')
      const name = document.createElement('span'); name.className = 'metric-label'; name.textContent = label; name.title = `蓝卡字段：${originalLabel}`
      const values = document.createElement('div'); values.className = 'metric-values'
      const currentMetric = document.createElement('span'); currentMetric.className = 'metric-current'; currentMetric.textContent = `直播累计 ${currentText}`
      const resultMetric = document.createElement('b'); resultMetric.className = `metric-result ${value.adjustments.includes(key) ? 'adjusted' : ''}`; resultMetric.textContent = changeText
      values.append(currentMetric, resultMetric); item.append(name, values); $('metrics').append(item)
    }
    $('submit').textContent = `确认并上报 · ${selectedName()}`
    $('report').classList.add('show')
    if (value.adjustments.length) {
      const labels = { watchTotal: '观看人数', interactionTotal: '互动人数', newFanTotal: '新增粉丝', dealTotal: '成交人数', gmvTotal: '成交金额', adCostTotal: '千川消耗', presaleOrderTotal: '预售订单' }
      feedback(`${value.adjustments.map(key => labels[key]).join('、')}较上次下降，已按平台校正值保留负差额。`, 'warn')
    } else feedback('')
  }
  const renderSuccess = value => {
    state.phase = 'success'; state.success = value
    $('workflow').classList.add('hidden'); $('success').classList.add('show')
    $('success-title').textContent = `${value.anchorName} 上报完成`
    $('success-range').textContent = `${formatTime(value.startTime)} 至 ${formatTime(value.endTime)}`
    $('success-mode').textContent = value.firstReport ? '本场首次上报，从开播时间起算' : '已按上一次上报计算差额'
    const values = [['观看', number(value.delta.watchTotal)], ['成交人数', number(value.delta.dealTotal)], ['成交金额', `¥${money(value.delta.gmvTotal)}`]]
    $('success-metrics').replaceChildren()
    for (const [label, metric] of values) { const cell = document.createElement('div'); const small = document.createElement('small'); const bold = document.createElement('b'); small.textContent = label; bold.textContent = metric; cell.append(small, bold); $('success-metrics').append(cell) }
  }
  const read = async () => {
    if (!selectedId() || state.busy) return
    state.busy = true; clearPreview(); feedback('正在读取蓝色卡片…'); renderBusy()
    try {
      state.context = await contextRequest()
      const record = await COLLECTOR.collect()
      if (record.platformSessionId !== mountedSessionId) throw new Error('页面已切换直播场次，请重新读取')
      const current = totals(record)
      state.preview = UI_STATE.preview(current, state.context.previousSnapshot, record.liveStartTime)
      state.preview.record = record
      state.preview.anchorUserId = selectedId()
      state.preview.anchorName = selectedName()
      renderPreview(state.preview)
      if (!state.preview.adjustments.length) feedback('')
    } catch (error) { feedback(error?.message || '读取数据失败', 'error') }
    finally { state.busy = false; renderBusy() }
  }
  const submit = async () => {
    const value = state.preview
    if (!value || state.busy) return
    if (selectedId() !== value.anchorUserId) return feedback('主播已变化，请重新读取数据。', 'error')
    if (Date.now() - Number(value.current.capturedAt || 0) > 120000) { clearPreview(); return feedback('读取结果已超过 2 分钟，请重新读取后上报。', 'error') }
    state.busy = true; feedback('正在提交本次主播数据，请勿重复点击…'); renderBusy()
    try {
      const latest = await contextRequest()
      if (Number(latest.previousSnapshot?.snapshotId || 0) !== Number(value.previousSnapshotId || 0)) throw new Error('本场已有新的上报，请重新读取后再提交')
      const current = value.current
      const result = await send({ type: 'JRM_TRANSITION', payload: {
        action: 'REPORT', roomName: value.record.roomName, platformSessionId: value.record.platformSessionId,
        liveStartTime: value.record.liveStartTime, anchorUserId: value.anchorUserId, previousSnapshotId: value.previousSnapshotId,
        capturedAt: current.capturedAt, idempotencyKey: `jrm-report-${crypto.randomUUID()}`,
        watchTotal: current.watchTotal, interactionTotal: current.interactionTotal, newFanTotal: current.newFanTotal,
        dealTotal: current.dealTotal, gmvTotal: current.gmvTotal, adCostTotal: current.adCostTotal,
        presaleOrderTotal: current.presaleOrderTotal, exposureWatchRate: current.exposureWatchRate,
        watchFollowRate: current.watchFollowRate, watchInteractionRate: current.watchInteractionRate, watchDealRate: current.watchDealRate
      } })
      const roomId = result.session?.roomId || state.context.roomId
      chrome.storage.local.set({ [selectedKey(roomId)]: value.anchorUserId }).catch(() => {})
      state.context = { ...result, roomMatched: true, roomId, lastReport: result.segments?.at(-1) || null }
      clearPreview(); renderSuccess(value)
    } catch (error) { clearPreview(); state.context = await contextRequest().catch(() => state.context); renderContext(); feedback(`${error?.message || '上报失败'}，请重新读取。`, 'error') }
    finally { state.busy = false; renderBusy() }
  }
  const next = async () => {
    if (state.busy) return
    state.busy = true; renderBusy()
    try {
      state.context = await contextRequest()
      await populateAnchors()
      $('anchor').value = ''
      state.success = null; clearPreview(); renderContext(); feedback('请选择下一位下播主播。')
    } catch (error) { showWorkflow(); feedback(error?.message || '同步失败', 'error') }
    finally { state.busy = false; renderBusy() }
  }
  $('anchor').onchange = async () => { clearPreview(); if (state.context?.roomId) await chrome.storage.local.set({ [selectedKey(state.context.roomId)]: selectedId() }); renderBusy() }
  $('read').onclick = read; $('submit').onclick = submit; $('next').onclick = next; $('refresh').onclick = load
  $('reselect').onclick = () => { clearPreview(); feedback('可重新选择主播并读取数据。') }
  $('open').onclick = () => send({ type: 'JRM_OPEN_LIVE_LIST' }).catch(error => feedback(error.message, 'error'))
  $('fold').onclick = () => root.querySelector('.panel').classList.toggle('collapsed')
  const listener = (message, _sender, respond) => { if (message?.type !== 'JRM_UI_TOGGLE') return false; root.querySelector('.panel').classList.toggle('collapsed'); respond({ handled: true }); return false }
  chrome.runtime.onMessage.addListener(listener)
  load()
  return { sessionId: mountedSessionId, destroy: () => { state.destroyed = true; clearTimeout(state.retryTimer); chrome.runtime.onMessage.removeListener(listener); host.remove() } }
}

let ui = null
function syncUi () {
  const sessionId = COLLECTOR?.roomIdFromUrl?.() || ''
  if (!isTarget()) { ui?.destroy(); ui = null; return }
  if (ui && ui.sessionId !== sessionId) { ui.destroy(); ui = null }
  if (!ui) ui = createUi()
}
if (document.documentElement) syncUi(); else document.addEventListener('DOMContentLoaded', syncUi, { once: true })
setInterval(syncUi, 1500)
