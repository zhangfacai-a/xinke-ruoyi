import request from '@/utils/request'

export function getPluginControl() {
  return request({
    url: '/live/plugin/control',
    method: 'get'
  })
}

export function updatePluginControl(data) {
  return request({
    url: '/live/plugin/control',
    method: 'put',
    data
  })
}

export function listPluginClients(query) {
  return request({
    url: '/live/plugin/client/list',
    method: 'get',
    params: query
  })
}

export function updatePluginClient(clientId, data) {
  return request({
    url: `/live/plugin/client/${encodeURIComponent(clientId)}`,
    method: 'put',
    data
  })
}
