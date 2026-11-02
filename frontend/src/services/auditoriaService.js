import api from './api'

export async function listarAuditoria({
  fecha,
  desde,
  hasta,
  operario,
  tipoEvento,
} = {}) {
  const params = {}

  if (fecha) params.fecha = fecha
  if (desde) params.desde = desde
  if (hasta) params.hasta = hasta
  if (operario) params.operario = operario
  if (tipoEvento) params.tipoEvento = tipoEvento

  const { data } = await api.get('/auditoria', {
    params: Object.keys(params).length > 0 ? params : undefined,
  })
  return data
}
