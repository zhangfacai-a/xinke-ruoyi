import request from '@/utils/request'

// 榜单事实
export const listAudienceRanks = query => request({ url: '/live/audience-rank/list', method: 'get', params: query })
export const getAudienceRankSummary = query => request({ url: '/live/audience-rank/summary', method: 'get', params: query })
export const listAudienceRankBatches = query => request({ url: '/live/audience-rank/batch/list', method: 'get', params: query })
export const getAudienceRankBatch = batchId => request({ url: `/live/audience-rank/batch/${batchId}`, method: 'get' })

// 跟单工作台
export const listAudienceFollowups = query => request({ url: '/live/audience-rank/followup/list', method: 'get', params: query })
export const getAudienceFollowupSummary = query => request({ url: '/live/audience-rank/followup/summary', method: 'get', params: query })
export const getAudienceTeamDashboard = query => request({ url: '/live/audience-rank/followup/dashboard', method: 'get', params: query })
export const getAudienceFollowup = followupId => request({ url: `/live/audience-rank/followup/${followupId}`, method: 'get' })
export const listAudienceFollowupLogs = followupId => request({ url: `/live/audience-rank/followup/${followupId}/logs`, method: 'get' })
export const listAudienceFollowupVisits = followupId => request({ url: `/live/audience-rank/followup/${followupId}/visits`, method: 'get' })
export const listAudienceCustomerOrders = followupId => request({ url: `/live/audience-rank/followup/${followupId}/orders`, method: 'get' })
export const listAudienceFollowupRooms = () => request({ url: '/live/audience-rank/followup/rooms', method: 'get' })
export const listAudienceFollowupAssignees = (roomId, roleCode) => request({ url: '/live/audience-rank/followup/assignees', method: 'get', params: { roomId, roleCode } })
export const saveAudienceFollowup = data => request({ url: `/live/audience-rank/followup/${data.followupId}`, method: 'post', data })
export const claimAudienceFollowup = followupId => request({ url: `/live/audience-rank/followup/${followupId}/claim`, method: 'post' })
export const reactivateAudienceFollowup = followupId => request({ url: `/live/audience-rank/followup/${followupId}/reactivate`, method: 'post' })
export const saveAudienceCustomerOrder = (followupId, data) => request({ url: `/live/audience-rank/followup/${followupId}/orders`, method: 'post', data })
export const updateAudienceFollowupStatus = (followupId, data) => request({ url: `/live/audience-rank/followup/${followupId}/status`, method: 'post', data })
export const batchUpdateAudienceFollowups = data => request({ url: '/live/audience-rank/followup/batch', method: 'put', data })
export const listAudienceAssignmentRules = () => request({ url: '/live/audience-rank/followup/assignment-rules', method: 'get' })
export const saveAudienceAssignmentRule = data => request({ url: `/live/audience-rank/followup/assignment-rules/${data.roomId}`, method: 'put', data })
export const autoAssignAudienceFollowups = roomId => request({ url: '/live/audience-rank/followup/auto-assign', method: 'post', data: { roomId } })
