import api from './api'

export async function listarOperarios({ soloActivos = false } = {}) {
  const { data } = await api.get('/operarios', {
    params: soloActivos ? { soloActivos: true } : undefined,
  })
  return data
}

export async function crearOperario(payload, idOperario) {
  const { data } = await api.post('/operarios', {
    ...payload,
    idOperario: idOperario ?? null,
  })
  return data
}

export async function actualizarOperario(idOperarioObjetivo, payload, idOperario) {
  const { data } = await api.put(`/operarios/${idOperarioObjetivo}`, {
    ...payload,
    idOperario: idOperario ?? null,
  })
  return data
}

export async function cambiarEstadoOperario(operario, estado, idOperario) {
  return actualizarOperario(
    operario.idOperario,
    {
      nombre: operario.nombre,
      email: operario.email,
      password: '',
      rol: operario.rol,
      estado,
    },
    idOperario,
  )
}
