(() => {
  const SOURCE = 'lc-live-customer-v2'
  const state = {
    running: false, sessionId: null, roomName: '', comments: 0, viewers: 0, pending: 0, collapsed: false,
    seenComments: new Map(), viewerSignatures: new Map(), viewerUsers: new Map(), viewerHeartbeatAt: new Map(), knownViewerNames: new Set(), pendingViewerNames: new Map(),
    viewerActivated: false, activationAttempted: false, viewerMode: 'idle', sessionFailures: 0,
    roomCandidate: '', roomCandidateHits: 0, roomConfidence: 0, uploadMessage: '', uploading: false, lastSyncAt: 0,
    scanTimer: null, viewerCheckTimer: null, activationTimer: null, commentObserver: null, audienceObserver: null,
    observedCommentContainer: null, observedAudienceContainer: null,
    lastViewerSnapshot: '', lastAudienceDomSnapshot: '', lastAudienceChangeAt: 0, blockedReason: '', unboundEvents: [],
    statusCode: 'CHECKING', statusMessage: '正在检查直播页面'
  }
  let root
  const normalize = value => String(value || '').replace(/\s+/g, ' ').trim()
  // Use the operator's local calendar date.  toISOString() is UTC and can
  // roll a China-based live session back to the previous day around midnight.
  const localDate = (value = new Date()) => {
    const date = value instanceof Date ? value : new Date(value)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }
  const controls = new Set(['互动评论', '设置', '暂无评论', '评论', '发送', '在线观众', '活跃观众动态'])
  const commentSelectors = '[class*="messageItem"],[class*="chatMessage"]'
  const audienceNameSelectors = '[class*="infoUserNickname"],[class*="userNickname"]'
  const statusText = {
    CHECKING: '正在检查直播页面', NEED_LOGIN: '尚未登录抖音，请先登录', WRONG_PAGE: '当前不是主播后台', ROOM_IDENTIFYING: '正在识别直播间', ROOM_MISSING: '未识别到有效直播间，请刷新页面', READY: '直播间已确认，可以采集', COLLECTING: '正在采集评论和观看用户', PAUSED: '采集已暂停', SYNCING: '正在同步数据', NETWORK_ERROR: '网络异常，数据已保存在本机', UPDATE_REQUIRED: '插件版本过低，已停止采集', SERVER_ERROR: '服务暂不可用，已暂停监控'
  }
  const id = () => crypto.randomUUID()
  const normalizeComment = value => String(value || '').normalize('NFKC').replace(/\s+/g, ' ').trim().toLowerCase().slice(0, 180)
  function sendMessage (message) {
    try {
      if (!chrome.runtime || !chrome.runtime.id) return Promise.resolve(null)
      return Promise.resolve(chrome.runtime.sendMessage(message)).catch(() => null)
    } catch (error) {
      return Promise.resolve(null)
    }
  }

  const hook = document.createElement('script')
  hook.src = chrome.runtime.getURL('page-hook.js')
  hook.onload = () => hook.remove()
  ;(document.head || document.documentElement).appendChild(hook)

  const roomReject = /累计|分钟|小时|在线观众|活跃观众|互动评论|评论|发送|设置|主播后台|数据分析|直播数据|观众分析|粉丝|人次|排行|暂无/
  function setStatus (code, message) { state.statusCode = code; state.statusMessage = message || statusText[code] || code; render() }
  function detectLoginState () {
    const path = `${location.pathname}${location.search}`.toLowerCase()
    if (/\/login|\/passport|\/account\/login|login_redirect/.test(path)) return false
    const body = normalize(document.body?.innerText || '').slice(0, 5000)
    if (/请登录后查看|登录后使用|立即登录/.test(body)) return false
    const loginButton = [...document.querySelectorAll('button,[role="button"],a')].find(node => node.offsetParent !== null && /^登录$|^立即登录$/.test(normalize(node.textContent)))
    return !loginButton
  }
  const roomSelectors = ['[data-e2e*="room-name"]', '[data-e2e*="live-room"]', '[class*="roomName"]', '[class*="room-name"]', '[class*="liveRoomName"]', '[class*="live-room-name"]']
  function roomCandidates () {
    const values = []
    const add = (value, score) => { const text = normalize(value); if (!text || text.length < 2 || text.length > 40 || roomReject.test(text)) return; if (!values.some(item => item.text === text)) values.push({ text, score }) }
    document.querySelectorAll('div[aria-haspopup="true"][class*="user-"] div[class*="info-"] span[class*="name-"]').forEach(node => {
      if (node.offsetParent !== null) add(node.textContent, 180)
    })
    roomSelectors.forEach(selector => document.querySelectorAll(selector).forEach(node => add(node.textContent || node.getAttribute('title') || node.getAttribute('aria-label'), 100)))
    document.querySelectorAll('h1,h2').forEach(node => { if (node.offsetParent !== null) add(node.textContent, 45) })
    document.querySelectorAll('[title],[aria-label]').forEach(node => { if (node.offsetParent !== null) add(node.getAttribute('title') || node.getAttribute('aria-label'), 25) })
    return values.sort((a, b) => b.score - a.score || a.text.length - b.text.length)
  }
  function roomName () {
    const candidate = roomCandidates()[0]
    if (!candidate) { state.roomCandidate = ''; state.roomCandidateHits = 0; state.roomConfidence = 0; return '' }
    if (candidate.text === state.roomCandidate) state.roomCandidateHits += 1
    else { state.roomCandidate = candidate.text; state.roomCandidateHits = 1 }
    state.roomConfidence = Math.min(100, candidate.score + Math.min(20, state.roomCandidateHits * 10))
    return candidate.score >= 150 || state.roomCandidateHits >= 2 ? candidate.text : ''
  }
  async function ensureSession () {
    const room = roomName()
    if (!room) { state.roomName = state.roomCandidate || ''; state.sessionId = null; setStatus(state.roomCandidate ? 'ROOM_IDENTIFYING' : 'ROOM_MISSING'); return false }
    if (state.sessionId && state.roomName === room) return true
    if (state.roomName && state.roomName !== room) {
      state.sessionId = null; state.seenComments.clear(); state.viewerSignatures.clear(); state.viewerUsers.clear(); state.viewerHeartbeatAt.clear(); state.knownViewerNames.clear(); state.pendingViewerNames.clear()
      state.viewerActivated = false; state.activationAttempted = false; state.viewerMode = 'idle'
      state.lastViewerSnapshot = ''; state.lastAudienceDomSnapshot = ''; state.lastAudienceChangeAt = 0; state.roomConfidence = 0; state.unboundEvents = []
    }
    state.roomName = room
    try {
      const result = await sendMessage({ type: 'LC_ENSURE_SESSION', session: { roomName: room, liveDate: localDate() } })
      const data = result?.data || result
      state.sessionId = data?.id || data?.sessionId || null
      if (!state.sessionId) { state.sessionFailures += 1; setStatus('SERVER_ERROR', '直播场次连接失败，请检查服务器'); if (state.sessionFailures >= 3) { state.blockedReason = '直播场次连接失败，已暂停监控'; stop() } return false }
      state.sessionFailures = 0
      setStatus('READY')
      if (state.unboundEvents.length) { const pending = state.unboundEvents.splice(0); queue(pending) }
      return true
    } catch (error) {
      state.sessionId = null
      state.sessionFailures += 1
      setStatus('NETWORK_ERROR', '无法连接服务器，数据会留在本机'); if (state.sessionFailures >= 3) { state.blockedReason = '直播场次连接失败，已暂停监控'; stop() }
      return false
    }
  }
  function queue (events) {
    if (!events.length) return
    if (!state.sessionId) { state.unboundEvents.push(...events.slice(-200)); return }
    sendMessage({ type: 'LC_QUEUE', events: events.map(event => ({ ...event, sessionId: state.sessionId, roomName: state.roomName })) }).then(result => { state.pending = result?.pending ?? state.pending; if (result?.ok === false) setStatus('NETWORK_ERROR', '数据已保存在本机，稍后自动重试'); render() })
  }
  function parseComment (node) {
    if (!node || node.closest('#lc-customer-hud')) return null
    const rawText = String(node.innerText || node.textContent || '').slice(0, 2000)
    const lines = rawText.split(/\r?\n/).map(normalize).filter(Boolean).filter(line => !controls.has(line) && !/^为保障您及其他用户/.test(line))
    if (lines.length < 2) return null
    const nickname = lines[0]; const content = lines.slice(1).join(' ')
    if (!nickname || !content || controls.has(nickname) || controls.has(content)) return null
    return { nickname, content, rawText }
  }
  function scanComments () {
    const container = findCommentContainer()
    if (!container) return
    const nodes = [...container.querySelectorAll(commentSelectors)].slice(-100)
    const events = []
    for (const node of nodes) {
      const value = parseComment(node); if (!value) continue
      const now = Date.now()
      const key = `${state.sessionId || 'pending'}|${normalize(value.nickname)}|${normalizeComment(value.content)}`
      const previous = state.seenComments.get(key)
      if (previous) continue
      state.seenComments.set(key, now)
      events.push({ eventId: id(), dedupKey: key, eventType: 'COMMENT', ...value, occurredAt: now, sourceChannel: 'PLUGIN_REALTIME', pageUrl: location.href })
    }
    state.comments += events.length; queue(events); render()
  }
  const visibleExactText = text => [...document.querySelectorAll('button,[role="tab"],[role="button"],div,span')]
    .find(node => normalize(node.textContent) === text && node.offsetParent !== null)

  async function refreshAudienceOnce () {
    if (!state.running || state.viewerActivated) return
    const online = visibleExactText('在线观众')
    if (!online) { state.activationAttempted = false; return }
    state.activationAttempted = true
    const activity = visibleExactText('活跃观众动态')
    const onlineItem = online.parentElement
    const alreadyActive = Boolean(onlineItem?.querySelector('[class*="itemActive"]'))
    if (alreadyActive && activity) {
      activity.click()
      await new Promise(resolve => setTimeout(resolve, 800))
    }
    if (state.running) online.click()
    render()
  }

  function scheduleAudienceActivation () {
    clearInterval(state.activationTimer)
    if (!state.running || state.viewerActivated) return
    state.activationTimer = setInterval(() => {
      if (!state.running || state.viewerActivated) { clearInterval(state.activationTimer); state.activationTimer = null; return }
      refreshAudienceOnce()
    }, 2500)
  }

  function normalizeViewer (item) {
    const user = item?.user || item?.user_info || item
    if (!user || typeof user !== 'object') return null
    const nickname = normalize(user.nickname || user.nick_name || item?.nickname || item?.nick_name)
    const secUid = user.sec_uid || user.secUid || item?.sec_uid || item?.secUid || null
    if (!nickname) return null
    return {
      nickname, secUid, rawUser: user,
      isFollower: user.is_follower ?? item?.is_follower,
      newImIconWithLevel: user.new_im_icon_with_level ?? item?.new_im_icon_with_level
    }
  }

  function findAudienceList (json) {
    const direct = [json?.data?.rank_list, json?.data?.audiences, json?.data?.audience_list, json?.data?.user_list, json?.rank_list]
    const matched = direct.find(list => Array.isArray(list) && list.some(item => normalizeViewer(item)?.nickname))
    if (matched) return matched
    const visit = (value, depth) => {
      if (!value || depth > 3) return null
      if (Array.isArray(value)) return value.some(item => normalizeViewer(item)?.secUid) ? value : null
      if (typeof value !== 'object') return null
      for (const child of Object.values(value)) { const result = visit(child, depth + 1); if (result) return result }
      return null
    }
    return visit(json, 0)
  }

  function consumeNetwork (payload) {
    if (!state.running || !state.sessionId) return
    let json; try { json = JSON.parse(payload.body) } catch (error) { return }
    const list = findAudienceList(json)
    if (!Array.isArray(list) || !list.length) return
    const users = list.map(normalizeViewer).filter(user => user?.secUid)
    if (!users.length) return
    state.viewerActivated = true
    const audienceAt = Date.now()
    state.viewerUsers = new Map(users.map(user => { user.lastListAt = audienceAt; return [user.secUid, user] }))
    for (const user of users) { state.knownViewerNames.add(user.nickname); state.pendingViewerNames.delete(user.nickname) }
    const snapshot = users.map(user => [user.secUid, user.nickname, user.isFollower ?? '', JSON.stringify(user.newImIconWithLevel ?? '')].join('|')).sort().join('\n')
    if (snapshot === state.lastViewerSnapshot) { queueViewerHeartbeats(users); render(); return }
    state.lastViewerSnapshot = snapshot
    const events = []
    const now = Date.now()
    for (const user of users) {
      const signature = [user.nickname, user.secUid, user.isFollower ?? '', JSON.stringify(user.newImIconWithLevel ?? '')].join('|')
      if (state.viewerSignatures.get(user.secUid) === signature) continue
      state.viewerSignatures.set(user.secUid, signature)
      state.viewerHeartbeatAt.set(user.secUid, now)
      events.push({ eventId: id(), dedupKey: `${state.sessionId}|${user.secUid}|${Math.floor(now / 180000)}`, eventType: 'VIEWER_PRESENCE', nickname: user.nickname, secUid: user.secUid, sec_uid: user.secUid, isFollower: user.isFollower, newImIconWithLevel: user.newImIconWithLevel, rawUser: JSON.stringify(user.rawUser).slice(0, 3000), occurredAt: now, sourceChannel: 'PLUGIN_REALTIME', pageUrl: location.href, roomName: state.roomName })
    }
    state.viewers += events.length; queue(events); queueViewerHeartbeats(users); render()
  }

  // The audience endpoint usually returns the same list repeatedly.  Keep
  // refreshing last_seen_at at a low, fixed rate so the server can calculate
  // a real viewing interval without uploading every DOM/API refresh.
  function queueViewerHeartbeats (users = [...state.viewerUsers.values()]) {
    if (!state.sessionId || !users.length) return
    const now = Date.now(); const bucket = Math.floor(now / 180000)
    const events = []
    for (const user of users) {
      if (!user?.secUid) continue
      if (user.lastListAt && now - user.lastListAt > 360000) continue
      const previous = Number(state.viewerHeartbeatAt.get(user.secUid) || 0)
      if (now - previous < 180000) continue
      state.viewerHeartbeatAt.set(user.secUid, now)
      events.push({ eventId: id(), dedupKey: `${state.sessionId}|${user.secUid}|${bucket}`, eventType: 'VIEWER_PRESENCE', nickname: user.nickname, secUid: user.secUid, sec_uid: user.secUid, isFollower: user.isFollower, newImIconWithLevel: user.newImIconWithLevel, occurredAt: now, sourceChannel: 'PLUGIN_REALTIME', pageUrl: location.href, roomName: state.roomName })
    }
    if (events.length) queue(events)
  }

  function findAudienceContainer () {
    return document.querySelector('#audienceListComp') || document.querySelector('[class*="AudienceContainer"] [role="grid"]')
  }

  function audienceDomNames (container) {
    return [...new Set([...container.querySelectorAll(audienceNameSelectors)].map(node => normalize(node.textContent)).filter(Boolean))]
  }

  function scheduleViewerCheck (delay) {
    clearTimeout(state.viewerCheckTimer)
    if (!state.running) return
    state.viewerCheckTimer = setTimeout(() => {
      scanAudienceDom()
      const stableFor = state.lastAudienceChangeAt ? Date.now() - state.lastAudienceChangeAt : Number.MAX_SAFE_INTEGER
      const nextDelay = stableFor < 30000 ? 5000 : (stableFor < 120000 ? 20000 : 60000)
      state.viewerMode = nextDelay === 5000 ? 'active' : (nextDelay === 20000 ? 'steady' : 'idle')
      scheduleViewerCheck(nextDelay)
      render()
    }, delay)
  }

  function scanAudienceDom () {
    const container = findAudienceContainer()
    if (!container) return
    const names = audienceDomNames(container)
    if (!names.length) return
    const snapshot = [...names].sort().join('\n')
    if (snapshot === state.lastAudienceDomSnapshot) return
    state.lastAudienceDomSnapshot = snapshot
    state.lastAudienceChangeAt = Date.now(); state.viewerMode = 'active'
    for (const nickname of names) {
      if (!state.knownViewerNames.has(nickname)) state.pendingViewerNames.set(nickname, state.pendingViewerNames.get(nickname) || Date.now())
    }
    scheduleViewerCheck(5000)
    render()
  }

  function startAudienceObserver () {
    const container = findAudienceContainer()
    if (!container || container === state.observedAudienceContainer) return
    state.audienceObserver?.disconnect()
    state.observedAudienceContainer = container
    state.audienceObserver = new MutationObserver(() => {
      if (!state.running) return
      clearTimeout(state.audienceScanDebounce)
      state.audienceScanDebounce = setTimeout(scanAudienceDom, 800)
    })
    state.audienceObserver.observe(container, { childList: true, subtree: true, characterData: true })
  }
  function findCommentContainer () {
    const first = document.querySelector(commentSelectors)
    if (first) {
      let current = first.parentElement; let fallback = current
      for (let i = 0; current && i < 6; i++, current = current.parentElement) {
        if (current !== document.body && current.querySelectorAll(commentSelectors).length >= 2) return current
      }
      if (fallback && fallback !== document.body) return fallback
    }
    const candidates = [...document.querySelectorAll('[class*="comment"],[class*="chat"],[data-e2e*="comment"],[role="log"]')]
    return candidates.find(node => node !== document.body && node.offsetParent !== null && node.querySelector(commentSelectors)) || null
  }
  function startCommentObserver () {
    const container = findCommentContainer()
    if (!container || container === state.observedCommentContainer) return
    state.commentObserver?.disconnect()
    state.observedCommentContainer = container
    state.commentObserver = new MutationObserver(() => {
      if (!state.running) return
      clearTimeout(state.scanDebounce)
      state.scanDebounce = setTimeout(scanComments, 300)
    })
    state.commentObserver.observe(container, { childList: true, subtree: true, characterData: true })
  }
  async function start () {
    if (state.running) return
    if (state.blockedReason) { render(); return }
    if (!detectLoginState()) { state.running = false; setStatus('NEED_LOGIN'); return }
    const serverState = await sendMessage({ type: 'LC_STATE' })
    state.pending = Number(serverState?.pending || 0)
    if (serverState?.requiredUpdate) { state.blockedReason = '插件版本过低，请更新后继续使用'; setStatus('UPDATE_REQUIRED'); return }
    if (serverState?.blocked) { state.blockedReason = `连续同步失败，已暂停监控${serverState.lastError ? `：${serverState.lastError}` : ''}`; setStatus('SERVER_ERROR', state.blockedReason); return }
    state.running = true; await ensureSession()
    startCommentObserver(); startAudienceObserver(); scanComments(); scanAudienceDom(); scheduleViewerCheck(5000)
    await refreshAudienceOnce(); scheduleAudienceActivation()
    state.scanTimer = setInterval(() => {
      if (!state.running) return
      ensureSession(); startCommentObserver(); const previousAudienceContainer = state.observedAudienceContainer; startAudienceObserver(); scanComments(); queueViewerHeartbeats();
      if (state.observedAudienceContainer !== previousAudienceContainer) scanAudienceDom()
      refreshAudienceOnce()
    }, 20000)
    if (state.sessionId) setStatus('COLLECTING'); else render()
  }
  function stop () {
    state.running = false
    clearInterval(state.scanTimer); clearTimeout(state.viewerCheckTimer); clearInterval(state.activationTimer); state.activationTimer = null; clearTimeout(state.scanDebounce); clearTimeout(state.audienceScanDebounce)
    state.commentObserver?.disconnect(); state.audienceObserver?.disconnect()
    state.commentObserver = null; state.audienceObserver = null
    state.observedCommentContainer = null; state.observedAudienceContainer = null; state.viewerMode = 'idle'
    if (!state.blockedReason) setStatus('PAUSED'); else render()
  }
  const clockText = value => value ? new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '尚未同步'
  function visualState () {
    if (state.blockedReason || ['NEED_LOGIN', 'UPDATE_REQUIRED', 'SERVER_ERROR'].includes(state.statusCode)) return 'error'
    if (state.uploading) return 'syncing'
    if (state.running && state.sessionId) return 'live'
    if (state.running) return 'checking'
    return 'paused'
  }
  function render () {
    if (!root) return
    const kind = visualState(); const pendingNames = state.pendingViewerNames.size
    root.dataset.state = kind
    root.classList.toggle('is-collapsed', state.collapsed)
    root.querySelector('#lc-status').textContent = kind === 'error' ? (state.statusCode === 'NEED_LOGIN' ? '需要登录' : '需要处理') : (kind === 'syncing' ? '正在同步' : (kind === 'live' ? '正在采集' : (kind === 'checking' ? '正在准备' : '已暂停')))
    root.querySelector('#lc-room').textContent = state.roomName || state.roomCandidate || '正在识别直播间'
    root.querySelector('#lc-comments').textContent = state.comments; root.querySelector('#lc-viewers').textContent = state.viewers; root.querySelector('#lc-pending').textContent = state.pending
    root.querySelector('#lc-ball-count').textContent = state.pending > 99 ? '99+' : String(state.pending || '')
    root.querySelector('#lc-ball-count').hidden = !state.pending
    root.querySelector('#lc-feedback').textContent = state.uploadMessage || state.blockedReason || state.statusMessage || statusText[state.statusCode] || ''
    root.querySelector('#lc-viewer-state').textContent = state.viewerActivated ? (pendingNames ? `在线观众已激活 · ${pendingNames} 人待补全` : '在线观众已激活') : (state.running ? '正在激活在线观众' : '在线观众等待启动')
    root.querySelector('#lc-sync-time').textContent = `最近同步：${clockText(state.lastSyncAt)}`
    root.querySelector('#lc-toggle').textContent = state.blockedReason ? '恢复监控' : (state.running ? '暂停' : '开始')
    root.querySelector('#lc-upload').textContent = state.uploading ? '同步中' : '立即同步'
    root.querySelector('#lc-upload').disabled = state.uploading
    root.querySelector('#lc-finish').disabled = state.uploading || (!state.running && !state.pending)
  }
  window.addEventListener('message', event => { if (event.source === window && event.origin === location.origin && event.data?.source === SOURCE && event.data.type === 'NETWORK_DATA') consumeNetwork(event.data.payload) })
  chrome.runtime.onMessage.addListener((message, sender, respond) => { if (message.type === 'LC_START') { start(); respond({ ok: true }) } else if (message.type === 'LC_STOP') { stop(); respond({ ok: true }) } else if (message.type === 'LC_GET_STATE') respond({ running: state.running, roomName: state.roomName, sessionId: state.sessionId, pending: state.pending, commentCount: state.comments, viewerCount: state.viewers, viewerActivated: state.viewerActivated, pendingViewerCount: state.pendingViewerNames.size, viewerMode: state.viewerMode, blockedReason: state.blockedReason, statusCode: state.statusCode, statusMessage: state.statusMessage }) })
  chrome.runtime.onMessage.addListener((message, sender, respond) => { if (message.type === 'LC_AUTO_STOP') { state.blockedReason = message.reason || '连续同步失败，已暂停监控'; stop(); state.blockedReason = message.reason || '连续同步失败，已暂停监控'; respond({ ok: true }) } else if (message.type === 'LC_RESET_MONITOR') { state.blockedReason = ''; state.sessionFailures = 0; render(); respond({ ok: true }) } })
  const style = document.createElement('style'); style.textContent = `#lc-customer-hud{position:fixed;right:20px;bottom:20px;z-index:2147483647;width:304px;background:#fff;color:#263244;border:1px solid #e2e7ed;border-radius:8px;box-shadow:0 12px 32px rgba(27,39,55,.2);font:13px -apple-system,BlinkMacSystemFont,"Segoe UI","Microsoft YaHei",sans-serif;user-select:none;overflow:hidden}#lc-customer-hud *{box-sizing:border-box}#lc-customer-hud .lc-top{display:flex;align-items:center;gap:9px;height:45px;padding:0 11px;border-bottom:1px solid #edf0f3;cursor:move;touch-action:none}#lc-customer-hud .lc-mark{position:relative;width:24px;height:24px;border:2px solid #eb6a31;border-radius:6px}#lc-customer-hud .lc-mark:before,#lc-customer-hud .lc-mark:after{position:absolute;content:"";background:#eb6a31}#lc-customer-hud .lc-mark:before{left:5px;top:5px;width:10px;height:2px;box-shadow:0 5px 0 #eb6a31,0 10px 0 #eb6a31}#lc-customer-hud .lc-mark:after{right:-4px;top:7px;width:4px;height:8px;border-radius:0 3px 3px 0}#lc-customer-hud .lc-brand{flex:1;font-weight:650}#lc-customer-hud #lc-status{display:flex;align-items:center;gap:5px;color:#667085;font-size:11px}#lc-customer-hud #lc-status:before{width:7px;height:7px;border-radius:50%;background:#98a2b3;content:""}#lc-customer-hud[data-state=live] #lc-status{color:#16835b}#lc-customer-hud[data-state=live] #lc-status:before{background:#22a06b;box-shadow:0 0 0 3px #e1f5ec}#lc-customer-hud[data-state=syncing] #lc-status:before,#lc-customer-hud[data-state=checking] #lc-status:before{background:#eb6a31;animation:lc-pulse 1.2s infinite}#lc-customer-hud[data-state=error] #lc-status{color:#c2412d}#lc-customer-hud[data-state=error] #lc-status:before{background:#d64545}#lc-customer-hud .lc-collapse{display:grid;place-items:center;width:26px;height:26px!important;padding:0!important;border:0!important;background:transparent!important;color:#7c8795!important;font-size:18px!important}#lc-customer-hud .lc-body{padding:12px}#lc-customer-hud .lc-room{overflow:hidden;color:#263244;font-weight:650;text-overflow:ellipsis;white-space:nowrap}#lc-customer-hud .lc-feedback{margin-top:4px;color:#7b8795;font-size:11px;line-height:16px}#lc-customer-hud[data-state=error] .lc-feedback{color:#c2412d}#lc-customer-hud .lc-metrics{display:grid;grid-template-columns:repeat(3,1fr);margin:12px 0;border:1px solid #e8ecf0;border-radius:6px}#lc-customer-hud .lc-metric{padding:9px 4px;text-align:center}#lc-customer-hud .lc-metric+.lc-metric{border-left:1px solid #e8ecf0}#lc-customer-hud .lc-metric strong{display:block;color:#172b4d;font-size:18px;line-height:22px}#lc-customer-hud .lc-metric span{color:#8b95a3;font-size:10px}#lc-customer-hud .lc-detail{display:flex;justify-content:space-between;gap:8px;margin:5px 0;color:#7d8794;font-size:11px}#lc-customer-hud .lc-actions{display:grid;grid-template-columns:1fr 1fr;gap:7px;margin-top:12px}#lc-customer-hud button{height:32px;border:0;border-radius:5px;padding:0 9px;background:#eb6a31;color:#fff;cursor:pointer;font-size:12px}#lc-customer-hud button.secondary{border:1px solid #d7dde5;background:#fff;color:#465468}#lc-customer-hud button.quiet{border:0;background:#f3f5f7;color:#5e6977}#lc-customer-hud button:disabled{opacity:.55;cursor:default}#lc-customer-hud .lc-ball{display:none}#lc-customer-hud.is-collapsed{width:54px;height:54px;border:0;border-radius:50%;background:transparent;box-shadow:none;overflow:visible;cursor:move;touch-action:none}#lc-customer-hud.is-collapsed .lc-panel{display:none}#lc-customer-hud.is-collapsed .lc-ball{position:relative;display:grid;place-items:center;width:54px;height:54px;border:3px solid #fff;border-radius:50%;background:#98a2b3;box-shadow:0 7px 20px rgba(30,42,56,.28)}#lc-customer-hud.is-collapsed .lc-ball:before{width:19px;height:15px;border:2px solid #fff;border-radius:4px;content:""}#lc-customer-hud.is-collapsed .lc-ball:after{position:absolute;top:17px;left:20px;width:14px;height:2px;background:#fff;box-shadow:0 5px 0 #fff,0 10px 0 #fff;content:""}#lc-customer-hud.is-collapsed[data-state=live] .lc-ball{background:#22a06b}#lc-customer-hud.is-collapsed[data-state=syncing] .lc-ball,#lc-customer-hud.is-collapsed[data-state=checking] .lc-ball{background:#eb6a31}#lc-customer-hud.is-collapsed[data-state=error] .lc-ball{background:#d64545}#lc-customer-hud .lc-ball-count{position:absolute;right:-3px;top:-4px;z-index:2;min-width:19px;height:19px;padding:0 4px;border:2px solid #fff;border-radius:10px;background:#263244;color:#fff;font:bold 10px/15px Arial;text-align:center}@keyframes lc-pulse{50%{opacity:.35}}`
  document.documentElement.appendChild(style)
  root = document.createElement('div'); root.id = 'lc-customer-hud'; root.innerHTML = '<div class="lc-ball" title="打开直播客户采集器"><span id="lc-ball-count" class="lc-ball-count" hidden></span></div><div class="lc-panel"><div class="lc-top"><span class="lc-mark"></span><span class="lc-brand">直播客户采集器</span><span id="lc-status">检查中</span><button class="lc-collapse" title="收起">−</button></div><div class="lc-body"><div id="lc-room" class="lc-room">正在识别直播间</div><div id="lc-feedback" class="lc-feedback">正在检查直播页面</div><div class="lc-metrics"><div class="lc-metric"><strong id="lc-comments">0</strong><span>评论用户</span></div><div class="lc-metric"><strong id="lc-viewers">0</strong><span>观看用户</span></div><div class="lc-metric"><strong id="lc-pending">0</strong><span>待同步</span></div></div><div class="lc-detail"><span id="lc-viewer-state">在线观众等待启动</span><span id="lc-sync-time">最近同步：尚未同步</span></div><div class="lc-actions"><button id="lc-toggle">开始</button><button id="lc-upload" class="secondary">立即同步</button><button id="lc-debug" class="quiet">查看采集</button><button id="lc-finish" class="quiet">结束本场</button></div></div></div>'
  document.documentElement.appendChild(root)
  root.querySelector('#lc-toggle').onclick = () => state.running ? stop() : start()
  async function manualUpload () { if (state.uploading) return; state.uploading = true; state.uploadMessage = '正在安全同步，请稍候'; render(); try { const result = await sendMessage({ type: 'LC_UPLOAD', manual: true }); if (!result || result.ok === false || result.error) state.uploadMessage = `同步失败：${result?.error || '插件后台暂不可用'}`; else { state.pending = result.pending ?? state.pending; state.lastSyncAt = result.sent > 0 ? Date.now() : state.lastSyncAt; state.uploadMessage = result.blocked ? '连续失败，已停止监控' : (result.deferred ? '同步过于频繁，请稍后再试' : `同步完成：成功 ${result.sent || 0} 条，剩余 ${result.pending || 0} 条`); if (result.blocked) state.blockedReason = `连续同步失败：${result.error || '请检查服务'}` } } catch (error) { state.uploadMessage = '同步失败：数据仍保存在本机' } finally { state.uploading = false; render(); setTimeout(() => { state.uploadMessage = ''; render() }, 5000) } }
  root.querySelector('#lc-upload').onclick = manualUpload
  root.querySelector('#lc-debug').onclick = () => sendMessage({ type: 'LC_OPEN_DEBUG' })
  root.querySelector('#lc-finish').onclick = async () => { if (!window.confirm('结束本场后会先同步剩余数据，再暂停采集。确定结束吗？')) return; await manualUpload(); stop(); state.uploadMessage = state.pending ? `本场已暂停，仍有 ${state.pending} 条等待网络恢复后同步` : '本场数据已同步并停止采集'; render() }
  const collapseButton = root.querySelector('.lc-collapse')
  const toggleCollapsed = event => { event?.stopPropagation(); state.collapsed = !state.collapsed; render(); persistDock() }
  collapseButton.onclick = toggleCollapsed
  const DOCK_KEY = 'lc_customer_hud_v3'; let drag = null; let dragMoved = false
  function persistDock () { try { const rect = root.getBoundingClientRect(); localStorage.setItem(DOCK_KEY, JSON.stringify({ x: rect.left, y: rect.top, collapsed: state.collapsed })) } catch (error) {} }
  function clampDock () { const rect = root.getBoundingClientRect(),x=Math.max(8,Math.min(innerWidth-root.offsetWidth-8,rect.left)),y=Math.max(8,Math.min(innerHeight-root.offsetHeight-8,rect.top));root.style.left=`${x}px`;root.style.top=`${y}px`;root.style.right='auto';root.style.bottom='auto' }
  try { const saved=JSON.parse(localStorage.getItem(DOCK_KEY)||'null');if(saved){state.collapsed=Boolean(saved.collapsed);root.style.left=`${Number(saved.x)||20}px`;root.style.top=`${Number(saved.y)||20}px`;root.style.right='auto';root.style.bottom='auto';setTimeout(clampDock,0)} } catch (error) {}
  root.querySelector('.lc-ball').onclick = event => { if (!dragMoved) toggleCollapsed(event); dragMoved=false }
  root.addEventListener('pointerdown', event => { const handle=state.collapsed?event.target.closest('.lc-ball'):event.target.closest('.lc-top');if(!handle||event.target.closest('button'))return;const rect=root.getBoundingClientRect();drag={x:event.clientX-rect.left,y:event.clientY-rect.top,startX:event.clientX,startY:event.clientY};dragMoved=false;root.setPointerCapture(event.pointerId);event.preventDefault() })
  root.addEventListener('pointermove', event => { if(!drag)return;if(Math.hypot(event.clientX-drag.startX,event.clientY-drag.startY)>4)dragMoved=true;const x=Math.max(8,Math.min(innerWidth-root.offsetWidth-8,event.clientX-drag.x)),y=Math.max(8,Math.min(innerHeight-root.offsetHeight-8,event.clientY-drag.y));root.style.left=`${x}px`;root.style.top=`${y}px`;root.style.right='auto';root.style.bottom='auto' })
  root.addEventListener('pointerup', event => { if(!drag)return;root.releasePointerCapture?.(event.pointerId);drag=null;persistDock() })
  window.addEventListener('resize', clampDock)
  render()
  setTimeout(start, 1200)
})()
