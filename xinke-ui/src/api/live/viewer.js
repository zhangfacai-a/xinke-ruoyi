import request from '@/utils/request'

export function listViewerLeads(query) {
  return request({
    url: '/live/viewer/lead/list',
    method: 'get',
    params: query
  })
}

export function getViewerLead(leadId) {
  return request({
    url: `/live/viewer/lead/${leadId}`,
    method: 'get'
  })
}

export function updateViewerLead(leadId, data) {
  return request({
    url: `/live/viewer/lead/${leadId}`,
    method: 'put',
    data
  })
}

export function addViewerFollow(leadId, data) {
  return request({
    url: `/live/viewer/lead/${leadId}/follow`,
    method: 'post',
    data
  })
}

export function getViewerSummary(query) {
  return request({
    url: '/live/viewer/summary',
    method: 'get',
    params: query
  })
}

export function getViewerBi(query) {
  return request({
    url: '/live/viewer/bi',
    method: 'get',
    params: query
  })
}

export function listViewerRoomSuggestions(query) {
  return request({
    url: '/live/viewer/room/suggestions',
    method: 'get',
    params: query
  })
}

export function listViewerOwnerSuggestions(query) {
  return request({
    url: '/live/viewer/owner/suggestions',
    method: 'get',
    params: query
  })
}

export function listLiveRooms(query) {
  return request({
    url: '/live/room/list',
    method: 'get',
    params: query
  })
}

export function getLiveRoom(roomKey) {
  return request({
    url: `/live/room/${encodeURIComponent(roomKey)}`,
    method: 'get'
  })
}

export function addLiveRoom(data) {
  return request({
    url: '/live/room',
    method: 'post',
    data
  })
}

export function updateLiveRoom(roomKey, data) {
  return request({
    url: `/live/room/${encodeURIComponent(roomKey)}`,
    method: 'put',
    data
  })
}

export function deleteLiveRoom(roomKey) {
  return request({
    url: `/live/room/${encodeURIComponent(roomKey)}`,
    method: 'delete'
  })
}

export function mapLiveRoomShop(roomKey, shopConfigId) {
  return request({
    url: `/live/rpa/room/${encodeURIComponent(roomKey)}/shop/${shopConfigId}`,
    method: 'put'
  })
}

export function unmapLiveRoomShop(roomKey) {
  return request({
    url: `/live/rpa/room/${encodeURIComponent(roomKey)}/shop`,
    method: 'delete'
  })
}

export function getRpaTrackingConfig() {
  return request({
    url: '/live/rpa/tracking/config',
    method: 'get'
  })
}

export function updateRpaTrackingConfig(data) {
  return request({
    url: '/live/rpa/tracking/config',
    method: 'put',
    data
  })
}

export function updateRpaViewerTracking(data) {
  return request({
    url: '/live/rpa/tracking/viewers',
    method: 'put',
    data
  })
}

export function listRpaWorkbench(query) {
  return request({
    url: '/live/rpa/workbench/list',
    method: 'get',
    params: query
  })
}

export function getRpaWorkbenchStats(query) {
  return request({
    url: '/live/rpa/workbench/stats',
    method: 'get',
    params: query
  })
}

export function listRpaWorkbenchShops() {
  return request({
    url: '/live/rpa/workbench/shops',
    method: 'get'
  })
}

export function listRpaUnmappedRooms() {
  return request({
    url: '/live/rpa/room/unmapped',
    method: 'get'
  })
}

export function addRpaShop(data) {
  return request({
    url: '/live/rpa/shop',
    method: 'post',
    data
  })
}

export function updateRpaShop(shopConfigId, data) {
  return request({
    url: `/live/rpa/shop/${shopConfigId}`,
    method: 'put',
    data
  })
}

export function bindRpaShopRooms(shopConfigId, roomKeys) {
  return request({
    url: `/live/rpa/shop/${shopConfigId}/rooms`,
    method: 'put',
    data: { roomKeys }
  })
}

export function enqueueRpaViewers(data) {
  return request({
    url: '/live/rpa/workbench/enqueue',
    method: 'post',
    data
  })
}

export function blacklistRpaViewers(data) {
  return request({
    url: '/live/rpa/workbench/blacklist',
    method: 'post',
    data
  })
}

export function restoreRpaBlacklist(ids) {
  return request({
    url: `/live/rpa/workbench/blacklist/${ids}`,
    method: 'delete'
  })
}
