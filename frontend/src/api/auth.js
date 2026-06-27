import { apiFetch, setAccessToken } from './client.js'

export async function login(username, password) {
  const data = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  setAccessToken(data.accessToken)
  return data
}

export function logout() {
  setAccessToken(null)
}
