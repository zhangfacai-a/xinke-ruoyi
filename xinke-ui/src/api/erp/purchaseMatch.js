import request from '@/utils/request'

export function listSupplier(query) {
  return request({ url: '/purchase/supplier/list', method: 'get', params: query })
}

export function supplierOptions() {
  return request({ url: '/purchase/supplier/options', method: 'get' })
}

export function addSupplier(data) {
  return request({ url: '/purchase/supplier', method: 'post', data })
}

export function updateSupplier(data) {
  return request({ url: '/purchase/supplier', method: 'put', data })
}

export function delSupplier(ids) {
  return request({ url: `/purchase/supplier/${ids}`, method: 'delete' })
}

export function listAlias(query) {
  return request({ url: '/purchase/supplier/alias/list', method: 'get', params: query })
}

export function addAlias(data) {
  return request({ url: '/purchase/supplier/alias', method: 'post', data })
}

export function updateAlias(data) {
  return request({ url: '/purchase/supplier/alias', method: 'put', data })
}

export function delAlias(ids) {
  return request({ url: `/purchase/supplier/alias/${ids}`, method: 'delete' })
}

export function listManual(query) {
  return request({ url: '/purchase/manual/list', method: 'get', params: query })
}

export function getManual(id) {
  return request({ url: `/purchase/manual/${id}`, method: 'get' })
}

export function addManual(data) {
  return request({ url: '/purchase/manual', method: 'post', data })
}

export function updateManual(data) {
  return request({ url: '/purchase/manual', method: 'put', data })
}

export function delManual(ids) {
  return request({ url: `/purchase/manual/${ids}`, method: 'delete' })
}

export function exportManual(query) {
  return request({ url: '/purchase/manual/export', method: 'post', data: query, responseType: 'blob', timeout: 60000 })
}

export function downloadManualTemplate() {
  return request({ url: '/purchase/manual/template', method: 'get', responseType: 'blob', timeout: 60000 })
}

export function listSummary(query) {
  return request({ url: '/purchase/summary/list', method: 'get', params: query })
}

export function summaryStats(query) {
  return request({ url: '/purchase/summary/stats', method: 'get', params: query })
}

export function updateSummary(data) {
  return request({ url: '/purchase/summary', method: 'put', data })
}

export function delSummary(ids) {
  return request({ url: `/purchase/summary/${ids}`, method: 'delete' })
}

export function matchSummary(id) {
  return request({ url: `/purchase/summary/${id}/match`, method: 'post' })
}

export function rematchFailed(query) {
  return request({ url: '/purchase/summary/rematch-failed', method: 'post', data: query })
}

export function manualBind(data) {
  return request({ url: `/purchase/summary/${data.summaryId}/manual-bind/${data.manualOrderId}`, method: 'post' })
}

export function exportSummary(query) {
  return request({ url: '/purchase/summary/export', method: 'post', data: query, responseType: 'blob', timeout: 60000 })
}

export function downloadSummaryTemplate() {
  return request({ url: '/purchase/summary/template', method: 'get', responseType: 'blob', timeout: 60000 })
}

export function previewImport(data, onUploadProgress) {
  return request({ url: '/purchase/import/preview', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000, onUploadProgress })
}

export function confirmImport(data, onUploadProgress) {
  return request({ url: '/purchase/import/confirm', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000, onUploadProgress })
}

export function listBatch(query) {
  return request({ url: '/purchase/import/batch/list', method: 'get', params: query })
}

export function getBatch(id) {
  return request({ url: `/purchase/import/batch/${id}`, method: 'get' })
}

export function listImportDetail(query) {
  return request({ url: '/purchase/import/detail/list', method: 'get', params: query })
}

export function listConflict(query) {
  return request({ url: '/purchase/conflict/list', method: 'get', params: query })
}

export function resolveConflict(data) {
  return request({ url: `/purchase/conflict/${data.conflictId}/resolve/${data.action}`, method: 'post' })
}
