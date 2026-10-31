import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

import { actualizarTarifa, obtenerTarifa } from '../services/tarifaService'
import { normalizarTarifa, TARIFA_POR_DEFECTO } from '../utils/tarifas'
import { useAuth } from './AuthContext'

const TarifaContext = createContext(null)

export function TarifaProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [tarifa, setTarifa] = useState(TARIFA_POR_DEFECTO)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')

  const recargar = useCallback(async () => {
    if (!isAuthenticated) return null
    setCargando(true)
    setError('')
    try {
      const data = await obtenerTarifa()
      const normalizada = normalizarTarifa(data)
      setTarifa(normalizada)
      return normalizada
    } catch {
      setError('No se pudo cargar el tarifario. Se usan los valores por defecto.')
      return null
    } finally {
      setCargando(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    if (!isAuthenticated) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTarifa(TARIFA_POR_DEFECTO)
      return
    }
    recargar()
  }, [isAuthenticated, recargar])

  const actualizar = useCallback(async (payload) => {
    const data = await actualizarTarifa(payload)
    const normalizada = normalizarTarifa(data)
    setTarifa(normalizada)
    setError('')
    return normalizada
  }, [])

  const value = useMemo(
    () => ({
      tarifa,
      cargando,
      error,
      recargar,
      actualizar,
    }),
    [tarifa, cargando, error, recargar, actualizar],
  )

  return <TarifaContext.Provider value={value}>{children}</TarifaContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useTarifa() {
  const context = useContext(TarifaContext)
  if (!context) {
    throw new Error('useTarifa debe usarse dentro de TarifaProvider')
  }
  return context
}
