import request from '@/utils/request'

export function listWarehouse(query) {
  return request({
    url: '/erp/warehouse/list',
    method: 'get',
    params: query
  })
}

export function getWarehouseSummary() {
  return request({
    url: '/erp/warehouse/summary',
    method: 'get'
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

export function listWarehouseLocation(warehouseId) {
  return request({
    url: `/erp/warehouse/${warehouseId}/location/list`,
    method: 'get'
  })
}

export function addWarehouseLocation(data) {
  return request({
    url: '/erp/warehouse/location',
    method: 'post',
    data
  })
}

export function updateWarehouseLocation(data) {
  return request({
    url: '/erp/warehouse/location',
    method: 'put',
    data
  })
}

export function delWarehouseLocation(locationIds) {
  return request({
    url: `/erp/warehouse/location/${locationIds}`,
    method: 'delete'
  })
}
