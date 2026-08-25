(() => {
  if (globalThis.__XINKE_AUDIENCE_PANEL__) return
  globalThis.__XINKE_AUDIENCE_PANEL__ = true

  const HOST_ID = 'xinke-audience-sync-host'
  const POSITION_KEY = 'xinkeAudiencePanelPosition'
  let host = null
  let panel = null
  let iframe = null
  let collapsed = false
  let dragState = null
  let dragShield = null

  function clamp(value, min, max) {
    return Math.min(Math.max(value, min), Math.max(min, max))
  }

  function applyPosition(left, top) {
    const width = panel?.offsetWidth || 380
    const height = panel?.offsetHeight || 620
    panel.style.left = `${clamp(left, 8, window.innerWidth - width - 8)}px`
    panel.style.top = `${clamp(top, 8, window.innerHeight - Math.min(height, 120) - 8)}px`
    panel.style.right = 'auto'
  }

  async function restorePosition() {
    try {
      const stored = await chrome.storage.local.get(POSITION_KEY)
      const value = stored[POSITION_KEY]
      if (Number.isFinite(value?.left) && Number.isFinite(value?.top)) {
        applyPosition(value.left, value.top)
      }
    } catch {
      // The default top-right position remains available.
    }
  }

  function persistPosition() {
    const rect = panel.getBoundingClientRect()
    chrome.storage.local.set({ [POSITION_KEY]: { left: rect.left, top: rect.top } }).catch(() => {})
  }

  function endDrag() {
    if (!dragState) return
    dragState = null
    dragShield?.remove()
    dragShield = null
    panel.classList.remove('dragging')
    persistPosition()
  }

  function startDrag(message) {
    if (!panel || collapsed) return
    const rect = panel.getBoundingClientRect()
    dragState = { startX: message.screenX, startY: message.screenY, left: rect.left, top: rect.top }
    panel.classList.add('dragging')
    dragShield = document.createElement('div')
    Object.assign(dragShield.style, { position: 'fixed', inset: '0', zIndex: '2147483647', cursor: 'move' })
    dragShield.addEventListener('pointermove', (event) => {
      applyPosition(dragState.left + event.screenX - dragState.startX, dragState.top + event.screenY - dragState.startY)
    })
    dragShield.addEventListener('pointerup', endDrag)
    dragShield.addEventListener('pointercancel', endDrag)
    host.shadowRoot.append(dragShield)
  }

  function setCollapsed(value) {
    collapsed = Boolean(value)
    panel.classList.toggle('collapsed', collapsed)
    iframe.contentWindow?.postMessage({ source: 'xinke-audience-host', type: 'COLLAPSED', value: collapsed }, '*')
  }

  function createPanel() {
    host = document.getElementById(HOST_ID)
    if (host) {
      panel = host.shadowRoot?.querySelector('.panel')
      iframe = host.shadowRoot?.querySelector('iframe')
      return
    }

    host = document.createElement('div')
    host.id = HOST_ID
    const shadow = host.attachShadow({ mode: 'open' })
    const style = document.createElement('style')
    style.textContent = `
      :host{all:initial}
      .panel{position:fixed;z-index:2147483646;top:86px;right:18px;width:380px;height:min(640px,calc(100vh - 104px));overflow:hidden;border:1px solid #dfe3e8;border-radius:7px;background:#fff;box-shadow:0 14px 42px rgba(18,28,45,.18),0 2px 8px rgba(18,28,45,.08);transition:height .16s ease,box-shadow .16s ease}
      .panel.dragging{transition:none;box-shadow:0 18px 50px rgba(18,28,45,.24)}
      .panel.collapsed{height:64px}
      iframe{display:block;width:100%;height:100%;border:0;background:#fff}
      @media(max-width:600px){.panel{top:8px;right:8px;width:calc(100vw - 16px);height:calc(100vh - 16px)}}
    `
    panel = document.createElement('div')
    panel.className = 'panel'
    iframe = document.createElement('iframe')
    iframe.src = chrome.runtime.getURL('popup.html?panel=1')
    iframe.title = '观众客户同步'
    panel.append(iframe)
    shadow.append(style, panel)
    document.documentElement.append(host)
    restorePosition()
  }

  function togglePanel() {
    createPanel()
    const hidden = panel.style.display === 'none'
    panel.style.display = hidden ? '' : 'none'
    if (hidden) setCollapsed(false)
  }

  chrome.runtime.onMessage.addListener((message) => {
    if (message?.type === 'XINKE_AUDIENCE_TOGGLE') togglePanel()
  })

  window.addEventListener('message', (event) => {
    const message = event.data
    if (event.source !== iframe?.contentWindow || message?.source !== 'xinke-audience-panel') return
    if (message.type === 'CLOSE') panel.style.display = 'none'
    if (message.type === 'COLLAPSE') setCollapsed(!collapsed)
    if (message.type === 'DRAG_START') startDrag(message)
  })

  window.addEventListener('resize', () => {
    if (!panel || panel.style.display === 'none') return
    const rect = panel.getBoundingClientRect()
    applyPosition(rect.left, rect.top)
  })
})()
