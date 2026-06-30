import { apiFetch } from './client.js'

export const register = (payload) =>
  apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(payload) })
export const currentUser = () => apiFetch('/auth/me')
export const listTopics = (page = 0, size = 20) => apiFetch(`/topics?page=${page}&size=${size}`)
export const openTopic = (id, replyPage = 0) => apiFetch(`/topics/${id}?replyPage=${replyPage}`)
export const listReplies = (topicId, page = 0, size = 10) =>
  apiFetch(`/topics/${topicId}/replies?page=${page}&size=${size}`)
export const createTopic = (payload) =>
  apiFetch('/topics', { method: 'POST', body: JSON.stringify(payload) })
export const updateTopic = (id, payload) =>
  apiFetch(`/topics/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const deleteTopic = (id, payload = {}) =>
  apiFetch(`/topics/${id}`, { method: 'DELETE', body: JSON.stringify(payload) })
export const purgeTopic = (id, payload) =>
  apiFetch(`/topics/${id}/purge`, { method: 'DELETE', body: JSON.stringify(payload) })
export const createReply = (topicId, content) =>
  apiFetch(`/topics/${topicId}/replies`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
export const updateReply = (id, content) =>
  apiFetch(`/replies/${id}`, { method: 'PUT', body: JSON.stringify({ content }) })
export const deleteReply = (id, payload = {}) =>
  apiFetch(`/replies/${id}`, { method: 'DELETE', body: JSON.stringify(payload) })
export const purgeReply = (id, payload) =>
  apiFetch(`/replies/${id}/purge`, { method: 'DELETE', body: JSON.stringify(payload) })
export const updateProfile = (payload) =>
  apiFetch('/users/me', { method: 'PUT', body: JSON.stringify(payload) })
export const changePassword = (payload) =>
  apiFetch('/users/me/password', { method: 'PUT', body: JSON.stringify(payload) })
export const listUsers = (page = 0, size = 20) => apiFetch(`/users?page=${page}&size=${size}`)
export const createUser = (payload) =>
  apiFetch('/users', { method: 'POST', body: JSON.stringify(payload) })
export const changeUserRole = (id, role) =>
  apiFetch(`/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role }) })
export const deleteUser = (id) => apiFetch(`/users/${id}`, { method: 'DELETE' })
export const restoreStatus = () => apiFetch('/ops/restore/status')
export const setRestoreMode = (enabled, reason) =>
  apiFetch(`/ops/restore/${enabled ? 'enable' : 'disable'}`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  })
