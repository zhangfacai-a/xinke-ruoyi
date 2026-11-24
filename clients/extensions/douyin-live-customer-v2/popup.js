const $ = id => document.getElementById(id)
const isNewerVersion = (latest, current) => {
  const left = String(latest || '').split('.').map(Number)
  const right = String(current || '').split('.').map(Number)
  for (let i = 0; i < Math.max(left.length, right.length); i++) {
    const difference = (left[i] || 0) - (right[i] || 0)
    if (difference) return difference > 0
  }
  return false
}

async function currentTab() {
  const tabs = await chrome.tabs.query({ active: true, currentWindow: true })
  return tabs[0]
}

const LIVE_URL = 'https://anchor.douyin.com/anchor/dashboard?from=default'
const isLivePage = url => /^https:\/\/anchor\.douyin\.com\/anchor\/(dashboard|analyze\/audience_rank)(?:[/?#]|$)/.test(url || '')
const openLivePage = tab => tab?.id ? chrome.tabs.update(tab.id, { url: LIVE_URL }) : null
const statusText = { CHECKING: '正在检查直播页面', NEED_LOGIN: '尚未登录抖音', WRONG_PAGE: '当前不是主播后台', ROOM_IDENTIFYING: '正在识别直播间', ROOM_MISSING: '未识别到直播间', READY: '直播间已确认', COLLECTING: '正在采集', PAUSED: '已暂停', NETWORK_ERROR: '网络异常，数据已保存在本机', UPDATE_REQUIRED: '插件版本过低', SERVER_ERROR: '服务异常，已暂停监控' }

async function refresh() {
  const tab = await currentTab()
  if (!tab || !isLivePage(tab.url)) {
    $('room').textContent = '当前不是直播采集页面'
    $('status').textContent = '不在目标页面'
    $('status-dot').className = 'dot off'
    $('toggle').textContent = '打开主播后台'
    $('toggle').onclick = () => openLivePage(tab)
    if (tab && /^https:\/\/anchor\.douyin\.com\/anchor\//.test(tab.url || '')) {
      $('message').textContent = '正在跳转到直播采集页...'
      setTimeout(() => openLivePage(tab), 250)
    }
    return
  }
  $('toggle').onclick = handleToggle
  chrome.tabs.sendMessage(tab.id, { type: 'LC_GET_STATE' }, async state => {
    if (chrome.runtime.lastError) {
      $('room').textContent = '请刷新当前抖音页面'
      $('status').textContent = '等待页面连接'
      $('status-dot').className = 'dot off'
      $('toggle').textContent = '刷新页面后重试'
      return
    }
    $('room').textContent = state?.roomName || (state?.statusCode === 'NEED_LOGIN' ? '请先登录抖音' : '直播间识别中')
    $('status').textContent = state?.statusMessage || statusText[state?.statusCode] || (state?.running ? '正在采集' : '已暂停')
    $('status-dot').className = state?.running ? 'dot' : 'dot off'
    $('toggle').textContent = state?.blockedReason ? (state?.statusCode === 'UPDATE_REQUIRED' ? '查看更新' : '恢复监控') : (state?.statusCode === 'NEED_LOGIN' ? '去登录抖音' : (state?.running ? '暂停采集' : '开始采集'))
    $('comments').textContent = state?.commentCount || 0
    $('viewers').textContent = state?.viewerCount || 0
    const queue = await chrome.runtime.sendMessage({ type: 'LC_STATE' })
    $('pending').textContent = queue?.pending ?? 0
    if (state?.statusCode === 'NEED_LOGIN') { $('message').textContent = '请先在这个专用窗口登录抖音，登录后再开始采集'; $('message').className = 'error' } else if (queue?.requiredUpdate) { $('message').textContent = `当前版本过低，最低要求 ${queue.update?.minVersion || '新版'}`; $('message').className = 'error' } else if (queue?.blocked) { $('message').textContent = `已自动停止监控：${queue.lastError || '连续同步失败'}`; $('message').className = 'error' } else if (queue?.failures) { $('message').textContent = `同步异常，已失败 ${queue.failures}/3 次`; $('message').className = 'error' } else if (state?.blockedReason) { $('message').textContent = state.blockedReason; $('message').className = 'error' } else if (state?.statusCode === 'NETWORK_ERROR') { $('message').textContent = state.statusMessage; $('message').className = 'error' } else { $('message').className = '' }
    if (queue?.update && (queue.requiredUpdate || isNewerVersion(queue.update.latestVersion, queue.version))) { $('message').textContent = queue.requiredUpdate ? `当前版本过低，最低要求 ${queue.update.minVersion}` : `有新版 ${queue.update.latestVersion}`; $('message').className = 'error'; $('update').hidden = false } else $('update').hidden = true
  })
}

async function handleToggle () {
  const tab = await currentTab()
  if (!tab) return
  if ($('toggle').textContent === '打开主播后台') { openLivePage(tab); return }
  if ($('toggle').textContent === '去登录抖音') { chrome.tabs.create({ url: 'https://www.douyin.com/' }); return }
  if ($('toggle').textContent === '查看更新') { $('update').click(); return }
  if ($('toggle').textContent === '恢复监控') await chrome.runtime.sendMessage({ type: 'LC_RESET_FAILURES' })
  chrome.tabs.sendMessage(tab.id, { type: $('toggle').textContent === '暂停采集' ? 'LC_STOP' : 'LC_START' }, () => setTimeout(refresh, 200))
}

$('upload').onclick = async () => {
  $('upload').disabled = true
  $('message').className = ''
  $('message').textContent = '正在同步...'
  try {
    const result = await chrome.runtime.sendMessage({ type: 'LC_UPLOAD' })
    if (!result || result.ok === false || result.error) {
      $('message').textContent = `同步失败：${result?.error || '无法连接插件后台'}`
      $('message').className = 'error'
    } else {
      $('message').textContent = result.blocked
        ? `已停止监控：${result.error || '连续同步失败'}`
        : `已同步 ${result.sent || 0} 条，剩余 ${result.pending || 0} 条`
      if (result.blocked) $('message').className = 'error'
    }
  } catch (error) {
    $('message').textContent = '同步失败：插件后台暂不可用，请稍后重试'
    $('message').className = 'error'
  } finally {
    $('upload').disabled = false
    refresh()
  }
}

$('open').onclick = () => chrome.tabs.create({ url: 'http://47.116.62.234/live-ops/customer-center' })
$('debug').onclick = () => chrome.runtime.sendMessage({ type: 'LC_OPEN_DEBUG' })
$('update').onclick = async () => { const state = await chrome.runtime.sendMessage({ type: 'LC_STATE' }); if (state?.update?.updateUrl) chrome.tabs.create({ url: state.update.updateUrl }); else $('message').textContent = '请联系管理员获取新版插件'
}

refresh()
setInterval(refresh, 2000)
