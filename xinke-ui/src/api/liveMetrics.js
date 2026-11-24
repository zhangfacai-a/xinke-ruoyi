import request from '@/utils/request'

export const listHandoverRooms = () => request({ url: '/live/metrics/rooms', method: 'get' })
export const getHandoverRoom = (roomId, params) => request({ url: `/live/metrics/rooms/${roomId}`, method: 'get', params })
export const addHandoverAnchor = (roomId, userId) => request({ url: `/live/metrics/rooms/${roomId}/anchors`, method: 'post', data: { userId } })
export const removeHandoverAnchor = (roomId, userId) => request({ url: `/live/metrics/rooms/${roomId}/anchors/${userId}`, method: 'delete' })
export const getHandoverSession = sessionId => request({ url: `/live/metrics/sessions/${sessionId}`, method: 'get' })
export const createHandoverSegment = (sessionId, data) => request({ url: `/live/metrics/sessions/${sessionId}/segments`, method: 'post', data })
export const updateHandoverSegment = (segmentId, data) => request({ url: `/live/metrics/segments/${segmentId}`, method: 'put', data })
export const voidHandoverSegment = (segmentId, data) => request({ url: `/live/metrics/segments/${segmentId}/void`, method: 'post', data })
