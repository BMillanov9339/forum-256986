const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:9000'

let accessToken = null

export function setAccessToken(token) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

export async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers ?? {})

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (response.status === 401) {
    setAccessToken(null)
  }

  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new ApiError(response.status, text)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

export class ApiError extends Error {
  constructor(status, body) {
    super(`API ${status}: ${body}`)
    this.status = status
    this.body = body
  }
}
