import api from './api'

export async function crearPedido(payload) {
  const { data } = await api.post('/pedidos', payload)
  return data
}

export async function listarPedidos(estado) {
  const { data } = await api.get('/pedidos', {
    params: estado ? { estado } : undefined,
  })
  return data
}

export async function obtenerResumen() {
  const { data } = await api.get('/pedidos/resumen')
  return data
}
