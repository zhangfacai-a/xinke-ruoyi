import request from '@/utils/request'

export function listOrder(query) {
  return request({
    url: '/erp/order/list',
    method: 'get',
    params: query
  })
}

export function getOrderItem(orderItemId) {
  return request({
    url: '/erp/order/' + orderItemId,
    method: 'get'
  })
}

export function listFulfillmentOrders(query) {
  return request({
    url: '/erp/order/fulfillment/list',
    method: 'get',
    params: query
  })
}

export function getFulfillmentSummary(query) {
  return request({
    url: '/erp/order/fulfillment/summary',
    method: 'get',
    params: query
  })
}

export function getFulfillmentDetail(salesId) {
  return request({
    url: '/erp/order/fulfillment/' + salesId,
    method: 'get'
  })
}

export function syncFulfillmentOrders() {
  return request({
    url: '/erp/order/fulfillment/sync',
    method: 'post'
  })
}

export function createShipment(data) {
  return request({
    url: '/erp/order/shipment',
    method: 'post',
    data
  })
}

export function shipShipment(shipmentNo) {
  return request({
    url: `/erp/order/shipment/${shipmentNo}/ship`,
    method: 'post'
  })
}

export function signShipment(shipmentNo) {
  return request({
    url: `/erp/order/shipment/${shipmentNo}/sign`,
    method: 'post'
  })
}

export function cancelShipment(shipmentNo, reason) {
  return request({
    url: `/erp/order/shipment/${shipmentNo}/cancel`,
    method: 'post',
    data: { reason }
  })
}

export function createAfterSale(data) {
  return request({
    url: '/erp/order/after-sale',
    method: 'post',
    data
  })
}

export function approveAfterSale(afterSaleNo) {
  return request({
    url: `/erp/order/after-sale/${afterSaleNo}/approve`,
    method: 'post'
  })
}

export function rejectAfterSale(afterSaleNo, reason) {
  return request({
    url: `/erp/order/after-sale/${afterSaleNo}/reject`,
    method: 'post',
    data: { reason }
  })
}

export function completeAfterSale(afterSaleNo) {
  return request({
    url: `/erp/order/after-sale/${afterSaleNo}/complete`,
    method: 'post'
  })
}
