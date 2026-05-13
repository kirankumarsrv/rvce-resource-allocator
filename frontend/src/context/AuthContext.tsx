import { createContext, ReactNode, useContext, useMemo, useState } from 'react'
import { getAccessToken as getStoredAccessToken, login as loginRequest, clearAuth, setAuthTokens } from '@/services/authService'
import type { AuthUser, LoginRequest } from '@/types/auth'

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  accessToken: string | null
  login: (credentials: LoginRequest) => Promise<AuthUser | null>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const decodeJwtPayload = (token: string): Record<string, unknown> | null => {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const decoded = atob(padded)
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

const parseUserFromToken = (token: string): AuthUser | null => {
  const payload = decodeJwtPayload(token)
  if (!payload) return null

  const email = typeof payload.email === 'string' ? payload.email : ''
  const rawRoles = payload.roles
  const roles = Array.isArray(rawRoles)
    ? rawRoles.filter((role): role is string => typeof role === 'string')
    : typeof rawRoles === 'string'
    ? [rawRoles]
    : []

  return {
    email,
    roles,
  }
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const initialToken = getStoredAccessToken()
  const [accessToken, setAccessToken] = useState<string | null>(initialToken)
  const [user, setUser] = useState<AuthUser | null>(
    initialToken ? parseUserFromToken(initialToken) : null
  )

  const login = async (credentials: LoginRequest): Promise<AuthUser | null> => {
    const loginResponse = await loginRequest(credentials)
    setAuthTokens(loginResponse)
    setAccessToken(loginResponse.accessToken)

    const authenticatedUser = parseUserFromToken(loginResponse.accessToken)
    setUser(authenticatedUser)
    return authenticatedUser
  }

  const logout = () => {
    clearAuth()
    setAccessToken(null)
    setUser(null)
  }

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: user !== null,
      accessToken,
      login,
      logout,
    }),
    [user, accessToken]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => { // eslint-disable-line react-refresh/only-export-components
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
