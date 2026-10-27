import api from './api'

export async function buscarPorDocumento(documento) {
  const { data } = await api.get('/clientes/buscar', { params: { doc: documento } })
  return data
}

export async function listarClientes() {
  const { data } = await api.get('/clientes')
  return data
}

export async function crearCliente(payload, idOperario) {
  const { data } = await api.post('/clientes', {
    ...payload,
    idOperario: idOperario ?? null,
  })
  return data
}

export async function actualizarCliente(idCliente, payload, idOperario) {
  const { data } = await api.put(`/clientes/${idCliente}`, {
    ...payload,
    idOperario: idOperario ?? null,
  })
  return data
}
