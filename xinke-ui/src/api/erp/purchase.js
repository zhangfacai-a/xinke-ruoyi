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
