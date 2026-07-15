import request from '@/utils/request'

export function listMovement(query) {
  return request({
    url: '/erp/movement/list',
    method: 'get',
    params: query
  })
}
