import request from '@/utils/request'

export function listSku(query) {
  return request({
    url: '/erp/sku/list',
    method: 'get',
    params: query
  })
}

export function getSku(skuId) {
  return request({
    url: '/erp/sku/' + skuId,
    method: 'get'
  })
}

export function addSku(data) {
  return request({
    url: '/erp/sku',
    method: 'post',
    data
  })
}

export function updateSku(data) {
  return request({
    url: '/erp/sku',
    method: 'put',
    data
  })
}

export function delSku(skuId) {
  return request({
    url: '/erp/sku/' + skuId,
    method: 'delete'
  })
}
