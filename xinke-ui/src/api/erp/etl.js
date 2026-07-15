import request from '@/utils/request'

export function buildOrderEtl(dt) {
  return request({
    url: '/erp/etl/order',
    method: 'post',
    params: { dt }
  })
}
