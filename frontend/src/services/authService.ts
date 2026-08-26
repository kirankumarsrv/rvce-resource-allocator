/**
 * Authentication service for login, logout, and token storage.
 */

import type { LoginRequest, LoginResponse } from '@/types/auth'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
export const apiUrl = (path: string): string =>
  /^https?:\/\//.test(path) ? path : `${API_BASE_URL}${path}`
const API_BASE = apiUrl('/api/auth')
const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

const getAuthHeaders = () => ({
  'Content-Type': 'application/json',
})

export const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
  const response = await fetch(`${API_BASE}/login`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Login failed')
  }

  return response.json()
}

/**
 * Refresh access token using refresh token
 */
export const refreshAccessToken = async (): Promise<LoginResponse> => {
  const refreshToken = getRefreshToken()
  const accessToken = getAccessToken()

  if (!refreshToken || !accessToken) {
    throw new Error('No refresh token available')
  }

  // Extract userId from current access token (assuming JWT structure)
  const payloadPart = accessToken.split('.')[1]
  const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  const payload = JSON.parse(atob(padded))
  const userId = payload.sub

  const response = await fetch(`${API_BASE}/refresh`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      userId: userId,
      refreshToken: refreshToken,
    }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Token refresh failed')
  }

  const newTokens = await response.json()
  setAuthTokens(newTokens)
  return newTokens
}

/**
 * Authenticated fetch wrapper with automatic token refresh
 */
export const authenticatedFetch = async (
  url: string,
  options: RequestInit = {}
): Promise<Response> => {
  const makeRequest = (token?: string) => {
    const headers: Record<string, string> = { ...options.headers as Record<string, string> }

    if (token) {
      headers.Authorization = `Bearer ${token}`
    } else {
      const authHeaders = createAuthHeaders()
      Object.assign(headers, authHeaders)
    }

    return fetch(apiUrl(url), {
      ...options,
      headers,
    })
  }

  let response = await makeRequest()

  // If we get a 401, try to refresh the token and retry once
  if (response.status === 401) {
    try {
      await refreshAccessToken()
      const newToken = getAccessToken()
      response = await makeRequest(newToken!)
    } catch {
      // If refresh fails, clear auth and rethrow
      clearAuth()
      throw new Error('Authentication failed - please log in again')
    }
  }

  return response
}

const createAuthHeaders = (): Record<string, string> => {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export const setAuthTokens = (loginResponse: LoginResponse): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken)
}

export const getAccessToken = (): string | null => {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export const getRefreshToken = (): string | null => {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export const logout = async (): Promise<void> => {
  const refreshToken = getRefreshToken()
  const accessToken = getAccessToken()

  if (!accessToken || !refreshToken) {
    return
  }

  const response = await fetch(`${API_BASE}/logout`, {
    method: 'POST',
    headers: {
      ...getAuthHeaders(),
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ refreshToken }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Logout failed')
  }
}

export const clearAuth = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
