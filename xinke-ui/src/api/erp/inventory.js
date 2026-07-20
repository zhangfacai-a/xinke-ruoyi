import request from '@/utils/request'

export function listInventory(query) {
  return request({
    url: '/erp/inventory/list',
    method: 'get',
    params: query
  })
}

export function getInventorySummary(query) {
  return request({
    url: '/erp/inventory/summary',
    method: 'get',
    params: query
  })
}

export function syncCloudInventory(data) {
  return request({
    url: '/erp/inventory/cloud/sync',
    method: 'post',
    data
  })
}

export function listCloudSyncLog(warehouseId) {
  return request({
    url: `/erp/inventory/cloud/sync/log/${warehouseId}`,
    method: 'get'
  })
}
