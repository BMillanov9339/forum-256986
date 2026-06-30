import { apiFetch, setAccessToken } from './client.js'

export async function login(identifier, password) {
  const data = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ identifier, password }),
  })
  setAccessToken(data.accessToken)
  return data
}

export function logout() {
  setAccessToken(null)
}
