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
