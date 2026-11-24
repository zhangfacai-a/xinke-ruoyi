import request from '@/utils/request'

export const listCustomerCenter = params => request({ url: '/live/v3/customer-center/customers', method: 'get', params })
export const getCustomerCenterDashboard = params => request({ url: '/live/v3/customer-center/dashboard', method: 'get', params })
export const exportCustomerCenter = params => request({
  url: '/live/v3/customer-center/export', method: 'get', params, responseType: 'blob', timeout: 120000
})

export const listLiveCustomers = params => request({ url: '/live/v2/customers', method: 'get', params })
export const searchLiveCustomerSuggestions = params => request({ url: '/live/v2/customers/suggestions', method: 'get', params, headers: { skipError: true } })
// The inbox endpoint is optional while older servers are being upgraded. Keep
// its error quiet so the center can fall back only for a genuine 404.
export const listLiveInbox = params => request({ url: '/live/v2/inbox', method: 'get', params, headers: { skipError: true, skipAuthDialog: true } })
export const getLiveInboxSummary = params => request({ url: '/live/v2/inbox/summary', method: 'get', params, headers: { skipError: true, skipAuthDialog: true } })
export const getLiveWorkbenchSummary = params => request({ url: '/live/v2/workbench/summary', method: 'get', params, headers: { skipError: true, skipAuthDialog: true } })
export const listLiveTasks = params => request({ url: '/live/v2/tasks', method: 'get', params })
export const listLiveOrders = params => request({ url: '/live/v2/orders', method: 'get', params })
export const listLiveUploadBatches = params => request({ url: '/live/v2/upload-batches', method: 'get', params })
export const saveLiveRegistration = data => request({ url: '/live/v2/registrations', method: 'post', data, headers: { skipError: true } })
export const getLiveOptions = () => request({ url: '/live/v2/options', method: 'get' })
export const ensureLiveSession = data => request({ url: '/live/v2/sessions/ensure', method: 'post', data })
export const getLiveCustomer = id => request({ url: `/live/v2/customers/${id}`, method: 'get' })
export const changeLiveCustomerOwner = (id, data) => request({ url: `/live/v2/customers/${id}/owner`, method: 'put', data })
export const actionLiveTask = (id, data) => request({ url: `/live/v2/tasks/${id}/action`, method: 'post', data })
export const runLivePhoneHandoff = () => request({ url: '/live/v2/tasks/phone-handoff/run', method: 'post' })
export const updateLiveOrder = (id, data) => request({ url: `/live/v2/orders/${id}`, method: 'put', data })
export const getLiveUploadBatch = id => request({ url: `/live/v2/upload-batches/${id}`, method: 'get' })
export const presignLiveAttachment = data => request({ url: '/live/v2/attachments/presign', method: 'post', data })
export const uploadLiveAttachment = (file, { customerId, opportunityId, orderId } = {}) => {
  if (!customerId) return Promise.reject(new Error('客户编号缺失，请保存客户后重试'))
  const data = new FormData()
  data.append('file', file)
  data.append('customerId', customerId)
  if (opportunityId) data.append('opportunityId', opportunityId)
  if (orderId) data.append('orderId', orderId)
  return request({
    url: '/live/v2/attachments/upload', method: 'post', data, timeout: 60000,
    headers: { 'Content-Type': 'multipart/form-data', skipError: true, repeatSubmit: false }
  })
}
export const completeLiveAttachment = (id, data = {}) => request({ url: `/live/v2/attachments/${id}/complete`, method: 'post', data })
export const getLiveAttachmentUrl = id => request({ url: `/live/v2/attachments/${id}/url`, method: 'get' })
