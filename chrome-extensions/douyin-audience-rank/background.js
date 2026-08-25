const TARGET_ORIGIN = 'https://anchor.douyin.com'
const TARGET_PATH = '/anchor/analyze/audience_rank'

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

updateAllTabs().catch(() => {})
