const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:9000/api/v1'

// Access tokens remain in memory so injected script cannot recover a token
// persisted by an earlier browser session.
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
    const body = await response.json().catch(() => ({}))
    throw new ApiError(response.status, body)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

export class ApiError extends Error {
  constructor(status, body) {
    super(body.message ?? `Request failed (${status})`)
    this.status = status
    this.body = body
  }
}
