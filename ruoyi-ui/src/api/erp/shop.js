import request from '@/utils/request'

export function listShop(query) {
  return request({
    url: '/erp/shop/list',
    method: 'get',
    params: query
  })
}

export function getShop(shopId) {
  return request({
    url: '/erp/shop/' + shopId,
    method: 'get'
  })
}

export function addShop(data) {
  return request({
    url: '/erp/shop',
    method: 'post',
    data
  })
}

export function updateShop(data) {
  return request({
    url: '/erp/shop',
    method: 'put',
    data
  })
}

export function delShop(shopId) {
  return request({
    url: '/erp/shop/' + shopId,
    method: 'delete'
  })
}
