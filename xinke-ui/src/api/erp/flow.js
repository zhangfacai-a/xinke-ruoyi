import request from '@/utils/request'

export function listFlow(flowType, query) {
  return request({
    url: `/erp/flow/${flowType}/list`,
    method: 'get',
    params: query
  })
}

export function getFlow(flowType, id) {
  return request({
    url: `/erp/flow/${flowType}/${id}`,
    method: 'get'
  })
}

export function addFlow(flowType, data) {
  return request({
    url: `/erp/flow/${flowType}`,
    method: 'post',
    data
  })
}

export function updateFlow(flowType, id, data) {
  return request({
    url: `/erp/flow/${flowType}/${id}`,
    method: 'put',
    data
  })
}

export function delFlow(flowType, ids) {
  return request({
    url: `/erp/flow/${flowType}/${ids}`,
    method: 'delete'
  })
}

export function approveFlow(flowType, bizNo) {
  return request({
    url: `/erp/flow/${flowType}/${bizNo}/approve`,
    method: 'post'
  })
}

export function inventoryIn(data) {
  return request({ url: '/erp/flow/inventory/in', method: 'post', data })
}

export function inventoryOut(data) {
  return request({ url: '/erp/flow/inventory/out', method: 'post', data })
}

export function inventoryTransfer(data) {
  return request({ url: '/erp/flow/inventory/transfer', method: 'post', data })
}

export function inventoryLoss(data) {
  return request({ url: '/erp/flow/inventory/loss', method: 'post', data })
}

export function generateSupplierPayable(reconcileNo) {
  return request({
    url: `/erp/flow/supplier-reconcile/${reconcileNo}/payable`,
    method: 'post'
  })
}
