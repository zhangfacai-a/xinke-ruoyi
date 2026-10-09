const CONFIG_URL = 'http://47.116.62.234'
const IS_COMPASS = location.hostname === 'compass.jinritemai.com'
const COLLECTOR = globalThis.XinkeJrmDomCollector
const METRICS = [
  ['live_show_cnt', '曝光人数'], ['watch_ucnt', '观看人数'], ['pay_ucnt', '成交人数'],
  ['follow_anchor_ucnt', '新增粉丝'], ['content_interaction_cnt', '互动人数']
]

const icon = name => ({
  report: '<path d="M6 3h12v18H6zM9 7h6M9 11h6M9 15h4"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  refresh: '<path d="M20 6v5h-5M18.5 15a7 7 0 1 1-.8-7.9L20 11"/>',
  upload: '<path d="M12 16V4m-5 5 5-5 5 5M5 14v6h14v-6"/>',
  external: '<path d="M14 4h6v6M20 4l-9 9M18 13v6H5V6h6"/>',
  target: '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="3"/>'
}[name] || '')
const svg = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${icon(name)}</svg>`

function styles () {
  return `<style>
  :host{--accent:#f56c2d;--ink:#252b36;--muted:#77808d;--line:#e8ebef;--soft:#f7f8fa;font-family:Inter,"Microsoft YaHei",sans-serif;color:var(--ink)}*{box-sizing:border-box;letter-spacing:0}.hidden{display:none!important}button{font:inherit;cursor:pointer}.icon{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round}
  .dock{position:fixed;right:18px;top:96px;width:58px;height:58px;border:3px solid #fff;border-radius:18px;background:linear-gradient(145deg,#ff813f,var(--accent));color:#fff;display:grid;place-items:center;box-shadow:0 12px 28px #d9512459;z-index:2147483647}.dock svg{width:27px;height:27px}.dock i{position:absolute;right:-2px;top:-2px;width:14px;height:14px;border:3px solid #fff;border-radius:50%;background:#9da5ae}.dock i.ready{background:#18a273}.dock i.pending{background:#e6a23c}.dock i.error{background:#d54a4a}
  .panel{position:fixed;right:18px;top:96px;width:370px;max-height:calc(100vh - 28px);overflow:hidden;border:1px solid #e3e6eb;border-radius:14px;background:#fff;box-shadow:0 20px 55px #1e293733;z-index:2147483647}.head{height:59px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);cursor:move}.brand{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:#fff1e9;color:var(--accent)}.brand svg{width:20px;height:20px}.title{min-width:0;flex:1}.title b{display:block;font-size:15px}.title span{display:block;margin-top:2px;color:var(--muted);font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.head button{width:30px;height:30px;border:0;background:transparent;color:#76808c}.body{padding:12px;overflow:auto;max-height:calc(100vh - 88px)}.status{display:flex;justify-content:space-between;align-items:center;padding:9px 10px;border-radius:8px;background:var(--soft);font-size:12px}.status span{display:flex;align-items:center;gap:7px}.signal{width:8px;height:8px;border-radius:50%;background:#a6adb7}.signal.ready{background:#18a273}.signal.pending{background:#e6a23c}.signal.error{background:#d54a4a}.room{display:grid;grid-template-columns:30px minmax(0,1fr) auto;align-items:center;gap:8px;margin-top:10px;padding:9px;border:1px solid var(--line);border-radius:8px}.room-mark{display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:#fff1e9;color:var(--accent)}.room small{display:block;color:var(--muted);font-size:10px}.room strong{display:block;margin-top:2px;font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.badge{padding:3px 6px;border-radius:5px;background:#eef1f4;color:#76808c;font-size:10px}.badge.ok{background:#eaf8f3;color:#087b5a}.section{margin-top:12px}.section-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:7px}.section-head b{font-size:12px}.section-head span{color:var(--muted);font-size:10px}.metrics{display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--line);border-radius:8px;overflow:hidden}.metric{padding:9px;border-right:1px solid var(--line);border-bottom:1px solid var(--line)}.metric:nth-child(3n){border-right:0}.metric:nth-last-child(-n+3){border-bottom:0}.metric span{display:block;color:var(--muted);font-size:10px}.metric strong{display:block;margin-top:4px;font-size:16px}.metric.feature{background:#fff4ed;color:#d94f18}.empty{grid-column:1/-1;padding:22px 10px;text-align:center;color:var(--muted);font-size:12px}.actions{display:grid;grid-template-columns:1fr 42px;gap:8px;margin-top:12px}.actions.confirm{grid-template-columns:1fr auto 42px}.primary,.secondary,.cancel{height:42px;border-radius:8px}.primary{display:flex;align-items:center;justify-content:center;gap:7px;border:0;background:linear-gradient(135deg,#ff7738,var(--accent));color:#fff;font-size:13px;font-weight:700}.primary:disabled{opacity:.55;cursor:not-allowed}.secondary,.cancel{display:grid;place-items:center;border:1px solid #dfe3e8;background:#fff;color:#697381}.cancel{padding:0 14px;font-size:12px}.feedback{display:none;margin-top:8px;padding:8px 9px;border-radius:7px;font-size:11px;line-height:17px}.feedback.show{display:block}.feedback.info{background:#fff7e8;color:#76501d}.feedback.success{background:#eaf8f3;color:#087b5a}.feedback.error{background:#fff0f0;color:#b73535}.landing{text-align:center;padding:26px 8px 15px}.landing .visual{width:70px;height:70px;margin:0 auto 13px;display:grid;place-items:center;border-radius:21px;background:#fff4ed;color:var(--accent)}.landing .visual svg{width:32px;height:32px}.landing h3{margin:0;font-size:16px}.landing p{margin:8px 0 16px;color:var(--muted);font-size:12px;line-height:19px}.launch{width:100%;height:42px;border:0;border-radius:8px;background:var(--accent);color:#fff;font-weight:700}.foot{display:flex;justify-content:space-between;margin-top:10px;color:#9aa1aa;font-size:10px}.foot button{border:0;background:transparent;color:#747e8a;font-size:10px}@media(max-width:480px){.panel{right:8px;top:8px;width:calc(100vw - 16px);max-height:calc(100vh - 16px)}.dock{right:12px;top:75px}}
  </style>`
}

function template () {
  return `<button class="dock" id="dock" title="打开直播数据采集"><span id="dockIcon">${svg('report')}</span><i id="dockDot"></i></button><aside class="panel hidden" id="panel"><header class="head" id="drag"><div class="brand">${svg('report')}</div><div class="title"><b>直播数据采集</b><span id="headStatus">检查当前页面</span></div><button id="close" title="收起">${svg('close')}</button></header><main class="body"><div class="landing hidden" id="landing"><div class="visual">${svg('target')}</div><h3>当前不是采集页面</h3><p>打开巨量百应直播详情页后，插件会自动读取五项经营数据。</p><button class="launch" id="launch">${svg('external')}　前往采集页</button></div><div id="collector"><div class="status"><span><i class="signal" id="signal"></i><b id="statusText">正在连接</b></span><span>待确认 <b id="pending">0</b></span></div><div class="room"><div class="room-mark">${svg('target')}</div><div><small>当前直播间</small><strong id="room">正在识别</strong></div><em class="badge" id="badge">识别中</em></div><section class="section"><div class="section-head"><b>本次经营数据</b><span id="time">尚未读取</span></div><div class="metrics" id="metrics"><div class="empty">点击“读取数据”开始</div></div></section><div class="actions" id="actions"><button class="primary" id="read">${svg('refresh')}<span>读取数据</span></button><button class="cancel hidden" id="cancel">取消</button><button class="secondary" id="reload" title="刷新页面">${svg('refresh')}</button></div><div class="feedback" id="feedback"></div></div><footer class="foot"><span id="version">版本 -</span><button id="open">打开采集页 ${svg('external')}</button></footer></main></aside>`
}

function mount () {
  if (document.getElementById('xinke-jrm-floating-host')) return
  const host = document.createElement('div'); host.id = 'xinke-jrm-floating-host'; host.attachShadow({ mode: 'open' }); host.shadowRoot.innerHTML = styles() + template(); document.documentElement.appendChild(host)
  const root = host.shadowRoot; const $ = id => root.getElementById(id)
  const state = { busy: false, latest: null, collapsed: true }
  const business = () => Boolean(COLLECTOR?.isMetricsRoute?.() && COLLECTOR?.hasSelectedSession?.())
  const setFeedback = (text, type = 'info') => { $('feedback').textContent = text; $('feedback').className = `feedback show ${type}` }
  const fmt = value => {
    // 采集器会保留页面原始单位（例如“1.21万”），不能直接 Number()，否则会显示 NaN。
    const raw = value && typeof value === 'object' ? value.value : value
    const text = String(raw ?? '').trim()
    if (!text || ['-', '--', '—', '暂无'].includes(text)) return '-'
    if (/[万亿]$/.test(text)) return text
    const normalized = text.replace(/,/g, '')
    const number = Number(normalized)
    return Number.isFinite(number) ? number.toLocaleString('zh-CN') : text
  }
  const metricMap = () => state.latest?.data?.metrics || {}
  const render = () => {
    const onPage = business(); $('landing').classList.toggle('hidden', onPage); $('collector').classList.toggle('hidden', !onPage)
    const metrics = metricMap(); $('room').textContent = state.latest?.roomName || (onPage ? '等待读取' : '请打开采集页'); $('time').textContent = state.latest?.liveStartTime || '尚未读取'; $('pending').textContent = state.latest ? '1' : '0'; $('version').textContent = '版本 2.0.1'
    $('statusText').textContent = !onPage ? '等待打开采集页' : state.latest ? '数据已读取，确认后上传' : '可以读取数据'; $('headStatus').textContent = $('statusText').textContent
    $('signal').className = `signal ${state.latest ? 'ready' : onPage ? 'pending' : ''}`; $('dockDot').className = state.busy ? 'pending' : state.latest ? 'ready' : ''
    $('badge').textContent = state.latest ? '已读取' : onPage ? '可采集' : '待进入'; $('badge').className = `badge${state.latest ? ' ok' : ''}`
    $('metrics').innerHTML = METRICS.some(([key]) => metrics[key]) ? METRICS.map(([key, label], index) => `<div class="metric${index === 1 ? ' feature' : ''}"><span>${label}</span><strong>${fmt(metrics[key]?.value)}</strong></div>`).join('') : '<div class="empty">点击“读取数据”开始</div>'
    $('read').disabled = state.busy || !onPage; $('read').querySelector('span').textContent = state.latest ? '确认上传' : '读取数据'; $('cancel').classList.toggle('hidden', !state.latest); $('actions').classList.toggle('confirm', Boolean(state.latest))
  }
  const openPage = () => { if (!business()) location.href = 'https://compass.jinritemai.com/talent/live-detail?from_page=/talent/live-overview' }
  const read = async () => {
    if (state.latest) return upload()
    state.busy = true; render(); setFeedback('正在读取漏斗和互动数据，请稍候…')
    try { const record = await COLLECTOR.collect({ onProgress: text => setFeedback(text) }); const result = await chrome.runtime.sendMessage({ type: 'JRM_DOM_CAPTURE', record }); if (!result?.ok) throw new Error(result?.error || '读取失败'); state.latest = result.latest; setFeedback('数据已读取，请核对后再次点击确认上传', 'info') } catch (error) { setFeedback(error?.message || '读取失败，请刷新页面后重试', 'error') } finally { state.busy = false; render() }
  }
  const upload = async () => { state.busy = true; render(); setFeedback('正在上传…'); try { const response = await chrome.runtime.sendMessage({ type: 'JRM_UPLOAD' }); if (!response?.ok) throw new Error(response?.error || '上传失败'); state.latest = null; const result = response.result || {}, shift = result.shift; if (result.shiftMatched && shift) setFeedback(`已匹配 ${shift.anchorName || '主播'} · ${String(shift.startTime || '').slice(11, 16)}–${String(shift.endTime || '').slice(11, 16)}`, 'success'); else if (result.roomMatched) setFeedback('数据已保存，但未匹配到班次，请联系管理员处理', 'error'); else setFeedback('数据已保存，但直播间尚未匹配，请联系管理员处理', 'error') } catch (error) { setFeedback(error?.message || '上传失败，请稍后重试', 'error') } finally { state.busy = false; render() } }
  $('read').onclick = read; $('cancel').onclick = async () => { await chrome.runtime.sendMessage({ type: 'JRM_DISCARD_DRAFT' }); state.latest = null; setFeedback('已取消本次数据'); render() }; $('reload').onclick = () => location.reload(); $('launch').onclick = openPage; $('open').onclick = openPage; $('close').onclick = () => { state.collapsed = true; $('panel').classList.add('hidden'); $('dock').classList.remove('hidden') }; $('dock').onclick = () => { state.collapsed = false; $('panel').classList.remove('hidden'); $('dock').classList.add('hidden'); render() }
  chrome.runtime.onMessage.addListener(message => { if (message?.type === 'JRM_UI_TOGGLE') $('dock').click() })
  render(); chrome.runtime.sendMessage({ type: 'JRM_STATE' }).then(result => { if (result?.latest) state.latest = result.latest; render() }).catch(() => {})
  setInterval(() => { if (!state.busy) render() }, 1500)
}

if (document.documentElement) mount(); else document.addEventListener('DOMContentLoaded', mount, { once: true })
