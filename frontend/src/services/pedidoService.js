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

export async function cambiarEstado(idPedido, payload) {
  const { data } = await api.patch(`/pedidos/${idPedido}/estado`, payload)
  return data
}

export async function resolverRevision(idPedido, payload) {
  const { data } = await api.put(`/pedidos/${idPedido}/revision`, payload)
  return data
}

/**
 * El backend devuelve application/pdf. Si rechaza la operación, el cuerpo
 * llega como JSON dentro de un Blob; lo convertimos para poder mostrar el
 * mensaje real del servidor.
 */
export async function imprimirRemito(idPedido, payload = {}) {
  try {
    const response = await api.post(`/remitos/${idPedido}/imprimir`, payload, {
      responseType: 'blob',
      timeout: 30000,
      headers: {
        Accept: 'application/pdf, application/json',
      },
    })
    return {
      blob: response.data,
      numero: response.headers?.['x-remito-numero'] ?? null,
      reimpresion: response.headers?.['x-remito-reimpresion'] === 'true',
    }
  } catch (error) {
    const data = error?.response?.data
    if (data instanceof Blob) {
      const texto = await data.text()
      try {
        error.response.data = JSON.parse(texto)
      } catch {
        error.response.data = { message: texto }
      }
    }
    throw error
  }
}
