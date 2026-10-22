export const SESSION_STORAGE_KEY = 'mandatodo.session'
export const UNAUTHORIZED_EVENT = 'mandatodo:unauthorized'

export function readSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY)
    if (!raw) return null

    const session = JSON.parse(raw)
    if (!session?.token) return null

    // Sesión vencida: se limpia para no enviar un token inválido.
    if (session.expiresAt && Date.now() >= session.expiresAt) {
      window.localStorage.removeItem(SESSION_STORAGE_KEY)
      return null
    }

    return session
  } catch {
    return null
  }
}

export function saveSession(session) {
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY)
}
