import request from '@/utils/request'

export function listInventory(query) {
  return request({
    url: '/erp/inventory/list',
    method: 'get',
    params: query
  })
}
