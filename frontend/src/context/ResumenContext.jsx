import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

import { getApiErrorMessage } from '../services/api'
import { obtenerResumen } from '../services/pedidoService'

const RESUMEN_INICIAL = {
  pendientes: 0,
  enRevision: 0,
  entregados: 0,
  clientes: 0,
}

const ResumenContext = createContext(null)

export function ResumenProvider({ children }) {
  const [resumen, setResumen] = useState(RESUMEN_INICIAL)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    try {
      const data = await obtenerResumen()
      setResumen({ ...RESUMEN_INICIAL, ...data })
      setError('')
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudieron actualizar las métricas.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh()
    const interval = window.setInterval(refresh, 60000)
    return () => window.clearInterval(interval)
  }, [refresh])

  const value = useMemo(
    () => ({ resumen, loading, error, refresh }),
    [resumen, loading, error, refresh],
  )

  return <ResumenContext.Provider value={value}>{children}</ResumenContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useResumen() {
  const context = useContext(ResumenContext)
  if (!context) {
    throw new Error('useResumen debe usarse dentro de ResumenProvider')
  }
  return context
}
