import request from '@/utils/request'

export function listWarehouse(query) {
  return request({
    url: '/erp/warehouse/list',
    method: 'get',
    params: query
  })
}

export function getWarehouse(warehouseId) {
  return request({
    url: '/erp/warehouse/' + warehouseId,
    method: 'get'
  })
}

export function addWarehouse(data) {
  return request({
    url: '/erp/warehouse',
    method: 'post',
    data
  })
}

export function updateWarehouse(data) {
  return request({
    url: '/erp/warehouse',
    method: 'put',
    data
  })
}

export function delWarehouse(warehouseId) {
  return request({
    url: '/erp/warehouse/' + warehouseId,
    method: 'delete'
  })
}
