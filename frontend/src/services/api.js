import axios from 'axios'

import { readSession, UNAUTHORIZED_EVENT } from '../utils/session'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 25000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const session = readSession()
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error?.config?.url ?? ''
    const esLogin = url.includes('/auth/login')

    if (error?.response?.status === 401 && !esLogin) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

export function getApiErrorMessage(error, fallback = 'Ocurrió un error inesperado.') {
  const data = error?.response?.data
  if (data?.message) return data.message
  if (Array.isArray(data?.details) && data.details.length > 0) return data.details[0]

  if (error?.code === 'ECONNABORTED') {
    return 'La operación tardó demasiado. Verificá tu conexión e intentá nuevamente.'
  }
  if (!error?.response) {
    return 'No se pudo conectar con el servidor. Verificá que el backend esté iniciado.'
  }
  if ([502, 503, 504].includes(error.response.status)) {
    return 'No se pudo conectar con el backend. Verificá que esté iniciado en http://localhost:8080.'
  }
  return fallback
}

export default api
