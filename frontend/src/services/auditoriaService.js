import api from './api'

export async function listarAuditoria({ fecha, operario, tipoEvento } = {}) {
  const params = {}

  if (fecha) params.fecha = fecha
  if (operario) params.operario = operario
  if (tipoEvento) params.tipoEvento = tipoEvento

  const { data } = await api.get('/auditoria', {
    params: Object.keys(params).length > 0 ? params : undefined,
  })
  return data
}
