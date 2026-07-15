import request from '@/utils/request'

export function listSupplier(query) {
  return request({
    url: '/erp/supplier/list',
    method: 'get',
    params: query
  })
}

export function getSupplier(supplierId) {
  return request({
    url: '/erp/supplier/' + supplierId,
    method: 'get'
  })
}

export function addSupplier(data) {
  return request({
    url: '/erp/supplier',
    method: 'post',
    data
  })
}

export function updateSupplier(data) {
  return request({
    url: '/erp/supplier',
    method: 'put',
    data
  })
}

export function delSupplier(supplierId) {
  return request({
    url: '/erp/supplier/' + supplierId,
    method: 'delete'
  })
}
