import api from './api'

export async function obtenerTarifa() {
  const { data } = await api.get('/tarifas')
  return data
}

export async function actualizarTarifa(payload) {
  const { data } = await api.put('/tarifas', payload)
  return data
}
