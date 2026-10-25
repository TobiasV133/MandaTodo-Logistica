import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

import * as authService from '../services/authService'
import {
  clearSession,
  readSession,
  saveSession,
  UNAUTHORIZED_EVENT,
} from '../utils/session'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readSession())

  const login = useCallback(async ({ username, password }) => {
    const data = await authService.login({ username, password })
    const nextSession = {
      token: data.token,
      tokenType: data.tokenType,
      expiresIn: data.expiresIn,
      expiresAt: Date.now() + Number(data.expiresIn ?? 0) * 1000,
      operario: data.operario,
    }
    saveSession(nextSession)
    setSession(nextSession)
    return nextSession
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  useEffect(() => {
    const onUnauthorized = () => logout()
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [logout])

  const value = useMemo(
    () => ({
      session,
      token: session?.token ?? null,
      operario: session?.operario ?? null,
      isAuthenticated: Boolean(session?.token),
      login,
      logout,
    }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return context
}
