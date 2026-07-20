import request from '@/utils/request'

export function listPurchase(query) {
  return request({
    url: '/erp/purchase/list',
    method: 'get',
    params: query
  })
}

export function getPurchase(purchaseId) {
  return request({
    url: '/erp/purchase/' + purchaseId,
    method: 'get'
  })
}

export function getPurchaseSummary() {
  return request({
    url: '/erp/purchase/summary',
    method: 'get'
  })
}

export function getSkuPurchaseHistory(skuId, limit = 20) {
  return request({
    url: `/erp/purchase/sku/${skuId}/history`,
    method: 'get',
    params: { limit }
  })
}

export function addPurchase(data) {
  return request({
    url: '/erp/purchase',
    method: 'post',
    data
  })
}

export function updatePurchase(data) {
  return request({
    url: '/erp/purchase',
    method: 'put',
    data
  })
}

export function delPurchase(purchaseId) {
  return request({
    url: '/erp/purchase/' + purchaseId,
    method: 'delete'
  })
}

export function updatePurchaseStatus(purchaseId, targetStatus) {
  return request({
    url: `/erp/purchase/${purchaseId}/status/${targetStatus}`,
    method: 'put'
  })
}

export function receivePurchase(purchaseId, data) {
  return request({
    url: `/erp/purchase/${purchaseId}/receive`,
    method: 'post',
    data
  })
}
