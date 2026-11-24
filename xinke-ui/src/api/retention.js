import request from '@/utils/request'

const root = '/live/retention'
export const retentionOptions = () => request({ url: `${root}/options` })
export const retentionList = params => request({ url: root, params })
export const retentionDashboard = params => request({ url: `${root}/dashboard`, params })
export const retentionDetail = id => request({ url: `${root}/${id}` })
export const retentionCreate = data => request({ url: root, method: 'post', data })
export const retentionBatchCheck = data => request({ url: `${root}/batch/check`, method: 'post', data })
export const retentionBatchCreate = data => request({ url: `${root}/batch`, method: 'post', data })
export const retentionBatchOwner = data => request({ url: `${root}/batch-owner`, method: 'put', data })
export const retentionUpdate = (id, data) => request({ url: `${root}/${id}`, method: 'put', data })
export const retentionDelete = (id, version) => request({ url: `${root}/${id}`, method: 'delete', params: { version } })
export const retentionUploadAttachment = (id, file) => {
  const data = new FormData()
  data.append('file', file)
  return request({ url: `${root}/${id}/attachments`, method: 'post', data, timeout: 60000, headers: { 'Content-Type': 'multipart/form-data' } })
}
export const retentionDeleteAttachment = (id, attachmentId) => request({ url: `${root}/${id}/attachments/${attachmentId}`, method: 'delete' })
export const retentionExport = params => request({ url: `${root}/export`, params, responseType: 'blob', timeout: 120000 })
