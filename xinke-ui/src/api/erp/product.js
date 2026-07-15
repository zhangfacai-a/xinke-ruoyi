import request from '@/utils/request'

export function listProduct(query) {
  return request({
    url: '/erp/product/list',
    method: 'get',
    params: query
  })
}

export function getProduct(productId) {
  return request({
    url: '/erp/product/' + productId,
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/erp/product',
    method: 'post',
    data
  })
}

export function updateProduct(data) {
  return request({
    url: '/erp/product',
    method: 'put',
    data
  })
}

export function delProduct(productId) {
  return request({
    url: '/erp/product/' + productId,
    method: 'delete'
  })
}
