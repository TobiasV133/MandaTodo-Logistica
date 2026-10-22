import api from './api'

export async function buscarPorDocumento(documento) {
  const { data } = await api.get('/clientes/buscar', { params: { doc: documento } })
  return data
}

export async function listarClientes() {
  const { data } = await api.get('/clientes')
  return data
}
