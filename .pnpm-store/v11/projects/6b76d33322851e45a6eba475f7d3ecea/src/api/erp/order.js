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
