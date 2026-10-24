import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import {
  ApiRequestError,
  cambiarEstadoPedido,
  getOperarioId,
  imprimirRemito,
  listarPedidos,
  type EstadoPedidoApi,
  type MotivoFallaApi,
  type PedidoApi,
} from './api'

const ESTADOS: { value: EstadoPedidoApi; label: string; badge: string }[] = [
  { value: 'PENDIENTE', label: 'Pendiente', badge: 'bg-[#E0F2FE] text-[#0369A1]' },
  { value: 'EN_REVISION', label: 'En revisión', badge: 'bg-[#FEF3C7] text-[#92400E]' },
  { value: 'EN_TRANSITO', label: 'En tránsito', badge: 'bg-[#DBEAFE] text-[#1D4ED8]' },
  { value: 'CANCELADO_EN_TRANSITO', label: 'Cancelado en tránsito', badge: 'bg-[#F1F5F9] text-[#475569]' },
  { value: 'ENTREGADO', label: 'Entregado', badge: 'bg-[#DCFCE7] text-[#15803D]' },
  { value: 'FALLIDO', label: 'Fallido', badge: 'bg-[#FEE2E2] text-[#B91C1C]' },
]

const MOTIVOS_FALLA: MotivoFallaApi[] = [
  'Ausente',
  'Dirección Inexistente',
  'Dirección Incompleta',
]

const FIELD = 'w-full rounded-lg border border-[#E2E8F0] bg-white px-3 py-2 text-sm text-[#0F172A] focus:border-[#2563EB] focus:outline-none disabled:cursor-not-allowed disabled:opacity-60'
const BUTTON_SECONDARY = 'rounded-lg border border-[#CBD5E1] bg-white px-3 py-2 text-xs font-medium text-[#334155] transition-colors hover:bg-[#F1F5F9] disabled:cursor-not-allowed disabled:opacity-50'

type FiltroEstado = EstadoPedidoApi | 'TODOS'

function estadoInfo(estado: EstadoPedidoApi) {
  return ESTADOS.find(item => item.value === estado) ?? ESTADOS[0]
}

function fechaCorta(fecha: string | null | undefined) {
  if (!fecha) return '—'
  const date = new Date(`${fecha.slice(0, 10)}T12:00:00`)
  return Number.isNaN(date.getTime())
    ? fecha
    : date.toLocaleDateString('es-AR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function fechaHora(fecha: string) {
  const date = new Date(fecha)
  return Number.isNaN(date.getTime())
    ? fecha
    : date.toLocaleString('es-AR', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
}

function moneda(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return '—'
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 2,
  }).format(value)
}

function cantidad(value: number | null | undefined, unit: string) {
  if (value == null || !Number.isFinite(value)) return '—'
  return `${new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 }).format(value)} ${unit}`
}

function humanizar(valor: string) {
  const texto = valor.replace(/_/g, ' ').toLocaleLowerCase('es-AR')
  return texto.charAt(0).toLocaleUpperCase('es-AR') + texto.slice(1)
}

function descargarPdf(blob: Blob, pedidoId: number) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `remito-R-${String(pedidoId).padStart(8, '0')}.pdf`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export default function GestionPedidos() {
  const [pedidos, setPedidos] = useState<PedidoApi[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<FiltroEstado>('TODOS')
  const [detailId, setDetailId] = useState<number | null>(null)
  const [changeId, setChangeId] = useState<number | null>(null)
  const [targetState, setTargetState] = useState<EstadoPedidoApi>('EN_TRANSITO')
  const [failureReason, setFailureReason] = useState<MotivoFallaApi>('Ausente')
  const [failureNote, setFailureNote] = useState('')
  const [cancelReason, setCancelReason] = useState('')
  const [changeError, setChangeError] = useState('')
  const [savingState, setSavingState] = useState(false)
  const [remitoId, setRemitoId] = useState<number | null>(null)
  const [remitoReason, setRemitoReason] = useState('')
  const [remitoKilometros, setRemitoKilometros] = useState('')
  const [remitoNeedsReason, setRemitoNeedsReason] = useState(false)
  const [remitoError, setRemitoError] = useState('')
  const [printing, setPrinting] = useState(false)
  const [notice, setNotice] = useState('')

  const loadPedidos = useCallback(async (signal?: AbortSignal) => {
    if (signal) setLoading(true)
    else setRefreshing(true)
    setLoadError('')

    try {
      const result = await listarPedidos(signal)
      setPedidos(result)
    } catch (error) {
      if (signal?.aborted) return
      setLoadError(error instanceof Error ? error.message : 'No se pudieron cargar los pedidos.')
    } finally {
      if (!signal?.aborted) {
        setLoading(false)
        setRefreshing(false)
      }
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void loadPedidos(controller.signal)
    return () => controller.abort()
  }, [loadPedidos])

  const visible = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('es-AR')
    return pedidos
      .filter(pedido => filter === 'TODOS' || pedido.estado === filter)
      .filter(pedido => !query || [
        pedido.idPedido,
        pedido.cliente,
        pedido.tipoCliente,
        pedido.descripcion,
        pedido.calle,
        pedido.localidad,
      ].join(' ').toLocaleLowerCase('es-AR').includes(query))
      .sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion))
  }, [pedidos, filter, search])

  const selected = pedidos.find(pedido => pedido.idPedido === detailId)
  const changing = pedidos.find(pedido => pedido.idPedido === changeId)
  const printingOrder = pedidos.find(pedido => pedido.idPedido === remitoId)

  function openChange(pedido: PedidoApi) {
    setDetailId(null)
    setChangeId(pedido.idPedido)
    setTargetState(pedido.estado === 'PENDIENTE' ? 'EN_TRANSITO' : 'PENDIENTE')
    setFailureReason('Ausente')
    setFailureNote('')
    setCancelReason('')
    setChangeError('')
  }

  function openRemito(pedido: PedidoApi) {
    setRemitoId(pedido.idPedido)
    setRemitoReason('')
    setRemitoKilometros('')
    setRemitoNeedsReason(false)
    setRemitoError('')
  }

  async function submitState(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!changing) return

    setSavingState(true)
    setChangeError('')
    try {
      const response = await cambiarEstadoPedido(changing.idPedido, {
        estado: targetState,
        operarioId: getOperarioId(),
        ...(targetState === 'FALLIDO'
          ? { motivoFalla: failureReason, ...(failureNote.trim() ? { notaFalla: failureNote.trim() } : {}) }
          : {}),
        ...(targetState === 'CANCELADO_EN_TRANSITO' ? { motivoCancelacion: cancelReason.trim() } : {}),
      })
      setPedidos(current => current.map(pedido =>
        pedido.idPedido === response.pedido.idPedido ? response.pedido : pedido
      ))
      setChangeId(null)
      if (response.reprogramado) {
        setNotice(
          `Entrega reprogramada para ${fechaCorta(response.fechaReprogramada)}. Recargo aplicado: ${moneda(response.recargoAplicado)}.`,
        )
      } else {
        setNotice(`Pedido #${response.pedido.idPedido}: estado actualizado a ${estadoInfo(response.pedido.estado).label}.`)
      }
    } catch (error) {
      setChangeError(error instanceof Error ? error.message : 'No se pudo actualizar el estado.')
    } finally {
      setSavingState(false)
    }
  }

  async function submitRemito(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!printingOrder) return

    const kilometros = remitoKilometros.trim() ? Number(remitoKilometros) : undefined
    if (kilometros !== undefined && (!Number.isFinite(kilometros) || kilometros < 0)) {
      setRemitoError('Ingresá una cantidad de kilómetros válida, igual o mayor a cero.')
      return
    }
    if (remitoNeedsReason && !remitoReason.trim()) {
      setRemitoError('El backend requiere el motivo para reimprimir este remito.')
      return
    }

    setPrinting(true)
    setRemitoError('')
    try {
      const blob = await imprimirRemito(printingOrder.idPedido, {
        operarioId: getOperarioId(),
        ...(remitoReason.trim() ? { motivoReimpresion: remitoReason.trim() } : {}),
        ...(kilometros !== undefined ? { kilometros } : {}),
      })
      descargarPdf(blob, printingOrder.idPedido)
      setRemitoId(null)
      setNotice(`Remito de pedido #${printingOrder.idPedido} descargado en PDF.`)
    } catch (error) {
      if (
        error instanceof ApiRequestError
        && error.status === 409
        && /motivo de reimpresión es obligatorio/i.test(error.message)
      ) {
        setRemitoNeedsReason(true)
        setRemitoError('Este pedido ya tiene un remito emitido. Indicá el motivo para registrar la reimpresión.')
      } else {
        setRemitoError(error instanceof Error ? error.message : 'No se pudo generar el remito.')
      }
    } finally {
      setPrinting(false)
    }
  }

  const conteo = (estado: EstadoPedidoApi) => pedidos.filter(pedido => pedido.estado === estado).length

  return (
    <section className="flex min-h-0 flex-1 flex-col overflow-hidden bg-[#F8F9FA]">
      <header className="flex flex-shrink-0 flex-wrap items-start justify-between gap-4 border-b border-[#E2E8F0] bg-white px-6 py-5">
        <div>
          <p className="text-[10px] font-mono uppercase tracking-[0.18em] text-[#64748B]">Operaciones / Seguimiento</p>
          <h1 className="mt-1 text-xl font-semibold text-[#0F172A]">Gestión de pedidos</h1>
          <p className="mt-1 text-sm text-[#64748B]">
            Pedidos conectados al backend · {visible.length} resultado{visible.length === 1 ? '' : 's'}
          </p>
        </div>
        <button type="button" onClick={() => void loadPedidos()} disabled={refreshing} className={BUTTON_SECONDARY}>
          {refreshing ? 'Actualizando…' : 'Actualizar datos'}
        </button>
      </header>

      <div className="flex-1 overflow-y-auto px-6 py-5">
        <div className="mx-auto max-w-7xl space-y-5">
          <div className="grid grid-cols-2 gap-3 xl:grid-cols-4">
            <SummaryCard label="Total de pedidos" value={pedidos.length} detail="Registros del backend" />
            <SummaryCard label="Pendientes" value={conteo('PENDIENTE')} detail="Listos para gestionar" tone="text-[#0369A1]" />
            <SummaryCard label="En tránsito" value={conteo('EN_TRANSITO')} detail="En curso de entrega" tone="text-[#1D4ED8]" />
            <SummaryCard label="Fallidos" value={conteo('FALLIDO')} detail="Con reprogramación registrada" tone="text-[#B91C1C]" />
          </div>

          <div className="space-y-3 rounded-xl border border-[#E2E8F0] bg-white p-4">
            <label className="block">
              <span className="mb-1.5 block text-xs font-medium text-[#475569]">Buscar pedidos</span>
              <input
                value={search}
                onChange={event => setSearch(event.target.value)}
                placeholder="Código, cliente, descripción o dirección…"
                className={FIELD}
              />
            </label>
            <div className="flex gap-1 overflow-x-auto pb-1" aria-label="Filtrar por estado">
              {([{ value: 'TODOS', label: 'Todos' }, ...ESTADOS] as { value: FiltroEstado; label: string }[]).map(item => {
                const count = item.value === 'TODOS' ? pedidos.length : conteo(item.value)
                const active = filter === item.value
                return (
                  <button
                    key={item.value}
                    type="button"
                    aria-pressed={active}
                    onClick={() => setFilter(item.value)}
                    className={`flex flex-shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-xs transition-colors ${
                      active ? 'bg-[#2563EB] text-white' : 'text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A]'
                    }`}
                  >
                    {item.label}
                    <span className={`font-mono ${active ? 'text-white/75' : 'text-[#94A3B8]'}`}>{count}</span>
                  </button>
                )
              })}
            </div>
          </div>

          {loadError && (
            <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-[#B91C1C]/20 bg-[#FEF2F2] px-4 py-3 text-sm text-[#991B1B]">
              <span>No se pudieron cargar los pedidos: {loadError}</span>
              <button type="button" onClick={() => void loadPedidos()} className="font-semibold underline">Reintentar</button>
            </div>
          )}

          <div className="overflow-hidden rounded-xl border border-[#E2E8F0] bg-white">
            {loading ? (
              <div className="p-12 text-center text-sm text-[#64748B]">Cargando pedidos del servidor…</div>
            ) : visible.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-sm font-medium text-[#334155]">{loadError ? 'No hay datos disponibles' : 'No se encontraron pedidos'}</p>
                <p className="mt-1 text-xs text-[#64748B]">
                  {loadError ? 'Verificá la conexión con el backend y volvé a intentar.' : 'Probá cambiar el filtro o el texto de búsqueda.'}
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1050px] text-left">
                  <thead className="border-b border-[#E2E8F0] bg-[#F8FAFC]">
                    <tr>
                      {['Pedido / fecha', 'Cliente', 'Envío', 'Destino', 'Estado', 'Entrega', ''].map(heading => (
                        <th key={heading} className="px-4 py-3 text-[10px] font-mono font-medium uppercase tracking-wider text-[#64748B]">{heading}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#E2E8F0]">
                    {visible.map(pedido => (
                      <tr key={pedido.idPedido} className="transition-colors hover:bg-[#F8FAFC]">
                        <td className="px-4 py-4 align-top">
                          <button type="button" onClick={() => setDetailId(pedido.idPedido)} className="font-mono text-xs font-medium text-[#2563EB] hover:underline">
                            #{pedido.idPedido}
                          </button>
                          <p className="mt-1 text-[11px] text-[#64748B]">{fechaHora(pedido.fechaCreacion)}</p>
                        </td>
                        <td className="max-w-52 px-4 py-4 align-top">
                          <p className="truncate text-sm font-medium text-[#0F172A]">{pedido.cliente}</p>
                          <p className="mt-1 text-[11px] text-[#64748B]">{pedido.tipoCliente}</p>
                        </td>
                        <td className="px-4 py-4 align-top">
                          <p className="max-w-56 truncate text-xs text-[#334155]" title={pedido.descripcion}>{pedido.descripcion}</p>
                          <p className="mt-1 text-[11px] text-[#64748B]">{cantidad(pedido.pesoKg, 'kg')} <span className="mx-1 text-[#CBD5E1]">·</span> {moneda(pedido.montoEnvio)}</p>
                        </td>
                        <td className="max-w-56 px-4 py-4 align-top">
                          <p className="truncate text-xs text-[#334155]">{pedido.calle} {pedido.altura ?? 'S/N'}{pedido.pisoDepto ? `, ${pedido.pisoDepto}` : ''}</p>
                          <p className="mt-1 truncate text-[11px] text-[#64748B]">{pedido.localidad}</p>
                        </td>
                        <td className="px-4 py-4 align-top">
                          <StatusBadge estado={pedido.estado} />
                          {pedido.motivoFalla && <p className="mt-1 text-[10px] text-[#B91C1C]">{pedido.motivoFalla}</p>}
                        </td>
                        <td className="px-4 py-4 align-top text-xs text-[#475569]">{fechaCorta(pedido.fechaProgramada)}</td>
                        <td className="px-4 py-4 align-top">
                          <div className="flex justify-end gap-2">
                            <button type="button" onClick={() => setDetailId(pedido.idPedido)} className={BUTTON_SECONDARY}>Detalle</button>
                            {pedido.estado !== 'EN_REVISION' && pedido.estado !== 'ENTREGADO' && pedido.estado !== 'CANCELADO_EN_TRANSITO' && (
                              <button type="button" onClick={() => openChange(pedido)} className="rounded-lg bg-[#2563EB] px-3 py-2 text-xs font-medium text-white transition-colors hover:bg-[#1D4ED8]">Estado</button>
                            )}
                            {pedido.estado !== 'EN_REVISION' && pedido.estado !== 'CANCELADO_EN_TRANSITO' && (
                              <button type="button" onClick={() => openRemito(pedido)} className={BUTTON_SECONDARY}>Remito PDF</button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>

      {selected && (
        <div className="fixed inset-0 z-[800] flex justify-end bg-[#0F172A]/45" onMouseDown={event => {
          if (event.target === event.currentTarget) setDetailId(null)
        }}>
          <aside className="h-full w-full max-w-xl overflow-y-auto border-l border-[#E2E8F0] bg-white shadow-2xl" aria-label={`Detalle del pedido ${selected.idPedido}`}>
            <div className="sticky top-0 z-10 flex items-start justify-between border-b border-[#E2E8F0] bg-white/95 px-6 py-5 backdrop-blur">
              <div>
                <p className="font-mono text-xs text-[#2563EB]">PEDIDO #{selected.idPedido}</p>
                <h2 className="mt-1 text-lg font-semibold text-[#0F172A]">Detalle del pedido</h2>
              </div>
              <button type="button" onClick={() => setDetailId(null)} className={BUTTON_SECONDARY}>Cerrar</button>
            </div>
            <div className="space-y-5 p-6">
              <section className="rounded-xl border border-[#E2E8F0] p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-semibold text-[#0F172A]">{selected.cliente}</p>
                    <p className="mt-1 text-xs text-[#64748B]">{selected.tipoCliente}</p>
                  </div>
                  <StatusBadge estado={selected.estado} />
                </div>
                <p className="mt-4 border-t border-[#E2E8F0] pt-4 text-sm text-[#334155]">{selected.descripcion}</p>
              </section>

              <section className="grid grid-cols-2 gap-3">
                <DetailValue label="Peso" value={cantidad(selected.pesoKg, 'kg')} />
                <DetailValue label="Monto de envío" value={moneda(selected.montoEnvio)} />
                <DetailValue label="Fecha de creación" value={fechaHora(selected.fechaCreacion)} />
                <DetailValue label="Fecha programada" value={fechaCorta(selected.fechaProgramada)} />
              </section>

              <section className="rounded-xl border border-[#E2E8F0] p-4">
                <p className="text-[10px] font-mono uppercase tracking-wider text-[#64748B]">Dirección de entrega</p>
                <p className="mt-2 text-sm font-medium text-[#0F172A]">{selected.calle} {selected.altura ?? 'S/N'}{selected.pisoDepto ? `, ${selected.pisoDepto}` : ''}</p>
                <p className="mt-1 text-xs text-[#64748B]">{selected.localidad}</p>
                {selected.latitud != null && selected.longitud != null && (
                  <p className="mt-2 font-mono text-[10px] text-[#64748B]">{selected.latitud}, {selected.longitud}</p>
                )}
              </section>

              {(selected.motivoFalla || selected.motivoCancelacion) && (
                <section className="rounded-xl border border-[#FECACA] bg-[#FEF2F2] p-4">
                  <p className="text-[10px] font-mono uppercase tracking-wider text-[#991B1B]">Incidencia</p>
                  <p className="mt-2 text-sm text-[#7F1D1D]">{selected.motivoFalla ?? selected.motivoCancelacion}</p>
                </section>
              )}

              {selected.revisiones.length > 0 && (
                <section className="rounded-xl border border-[#FDE68A] bg-[#FFFBEB] p-4">
                  <p className="text-[10px] font-mono uppercase tracking-wider text-[#92400E]">Historial de revisión</p>
                  <ul className="mt-2 space-y-2">
                    {selected.revisiones.map(revision => (
                      <li key={revision.idRevision} className="flex items-center justify-between gap-3 text-xs text-[#78350F]">
                        <span>{humanizar(revision.motivoRetencion)}</span>
                        <span className="font-mono text-[10px]">{humanizar(revision.estadoResolucion)}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              )}

              <div className="flex flex-wrap gap-2 border-t border-[#E2E8F0] pt-5">
                {selected.estado !== 'EN_REVISION' && selected.estado !== 'ENTREGADO' && selected.estado !== 'CANCELADO_EN_TRANSITO' && (
                  <button type="button" onClick={() => openChange(selected)} className="rounded-lg bg-[#2563EB] px-4 py-2.5 text-xs font-medium text-white hover:bg-[#1D4ED8]">Cambiar estado</button>
                )}
                {selected.estado !== 'EN_REVISION' && selected.estado !== 'CANCELADO_EN_TRANSITO' && (
                  <button type="button" onClick={() => openRemito(selected)} className={BUTTON_SECONDARY}>Generar / reimprimir remito</button>
                )}
              </div>
            </div>
          </aside>
        </div>
      )}

      {changing && (
        <Modal title={`Cambiar estado · Pedido #${changing.idPedido}`} onClose={() => !savingState && setChangeId(null)}>
          <form onSubmit={submitState} className="space-y-4">
            <label className="block">
              <span className="mb-1.5 block text-xs font-medium text-[#475569]">Nuevo estado</span>
              <select value={targetState} onChange={event => setTargetState(event.target.value as EstadoPedidoApi)} className={FIELD}>
                {ESTADOS.filter(estado => estado.value !== 'EN_REVISION' && estado.value !== changing.estado).map(estado => (
                  <option key={estado.value} value={estado.value}>{estado.label}</option>
                ))}
              </select>
            </label>

            {targetState === 'FALLIDO' && (
              <>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-medium text-[#475569]">Motivo de falla <span className="text-[#B91C1C]">*</span></span>
                  <select required value={failureReason} onChange={event => setFailureReason(event.target.value as MotivoFallaApi)} className={FIELD}>
                    {MOTIVOS_FALLA.map(motivo => <option key={motivo} value={motivo}>{motivo}</option>)}
                  </select>
                </label>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-medium text-[#475569]">Nota (opcional)</span>
                  <textarea value={failureNote} onChange={event => setFailureNote(event.target.value)} maxLength={500} rows={3} className={FIELD} />
                </label>
                <p className="rounded-lg border border-[#FDE68A] bg-[#FFFBEB] px-3 py-2 text-xs text-[#92400E]">
                  El backend aplicará el recargo y calculará el siguiente día hábil al confirmar.
                </p>
              </>
            )}

            {targetState === 'CANCELADO_EN_TRANSITO' && (
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium text-[#475569]">Motivo de cancelación <span className="text-[#B91C1C]">*</span></span>
                <textarea required value={cancelReason} onChange={event => setCancelReason(event.target.value)} maxLength={255} rows={3} className={FIELD} />
              </label>
            )}

            {changeError && <p role="alert" className="rounded-lg border border-[#FECACA] bg-[#FEF2F2] px-3 py-2 text-xs text-[#991B1B]">{changeError}</p>}
            <div className="flex justify-end gap-2 border-t border-[#E2E8F0] pt-4">
              <button type="button" onClick={() => setChangeId(null)} disabled={savingState} className={BUTTON_SECONDARY}>Volver</button>
              <button type="submit" disabled={savingState} className="rounded-lg bg-[#2563EB] px-4 py-2 text-xs font-medium text-white hover:bg-[#1D4ED8] disabled:opacity-50">
                {savingState ? 'Guardando…' : 'Confirmar cambio'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {printingOrder && (
        <Modal title={`Remito · Pedido #${printingOrder.idPedido}`} onClose={() => !printing && setRemitoId(null)}>
          <form onSubmit={submitRemito} className="space-y-4">
            <p className="text-sm leading-6 text-[#475569]">
              El servidor genera el PDF con los datos del cliente, dirección, carga, kilos y espacio para firma.
              Los kilómetros no se guardan en pedidos; podés informarlos para incluirlos en este remito.
            </p>
            <label className="block">
              <span className="mb-1.5 block text-xs font-medium text-[#475569]">Kilómetros (opcional)</span>
              <input type="number" min="0" step="0.01" value={remitoKilometros} onChange={event => setRemitoKilometros(event.target.value)} className={FIELD} />
            </label>
            <label className="block">
              <span className="mb-1.5 block text-xs font-medium text-[#475569]">
                Motivo de reimpresión {remitoNeedsReason ? <span className="text-[#B91C1C]">*</span> : <span className="font-normal text-[#64748B]">(solo si ya se emitió)</span>}
              </span>
              <input
                required={remitoNeedsReason}
                value={remitoReason}
                onChange={event => setRemitoReason(event.target.value)}
                maxLength={200}
                placeholder="Ej.: extravío del remito original"
                className={FIELD}
              />
            </label>
            {remitoError && <p role="alert" className="rounded-lg border border-[#FECACA] bg-[#FEF2F2] px-3 py-2 text-xs text-[#991B1B]">{remitoError}</p>}
            <div className="flex justify-end gap-2 border-t border-[#E2E8F0] pt-4">
              <button type="button" onClick={() => setRemitoId(null)} disabled={printing} className={BUTTON_SECONDARY}>Cancelar</button>
              <button type="submit" disabled={printing} className="rounded-lg bg-[#2563EB] px-4 py-2 text-xs font-medium text-white hover:bg-[#1D4ED8] disabled:opacity-50">
                {printing ? 'Generando PDF…' : 'Generar y descargar PDF'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {notice && (
        <div role="status" className="fixed bottom-5 right-5 z-[1200] max-w-md rounded-lg border border-[#BFDBFE] bg-white px-4 py-3 text-sm text-[#0F172A] shadow-lg">
          {notice}
          <button type="button" onClick={() => setNotice('')} className="ml-3 font-semibold text-[#64748B]" aria-label="Cerrar aviso">×</button>
        </div>
      )}
    </section>
  )
}

function SummaryCard({ label, value, detail, tone = 'text-[#0F172A]' }: { label: string; value: number; detail: string; tone?: string }) {
  return (
    <article className="rounded-xl border border-[#E2E8F0] bg-white p-4">
      <p className="text-xs font-medium text-[#64748B]">{label}</p>
      <p className={`mt-2 font-mono text-2xl font-semibold ${tone}`}>{value}</p>
      <p className="mt-1 text-[11px] text-[#94A3B8]">{detail}</p>
    </article>
  )
}

function StatusBadge({ estado }: { estado: EstadoPedidoApi }) {
  const info = estadoInfo(estado)
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-[10px] font-medium ${info.badge}`}>{info.label}</span>
}

function DetailValue({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-[#E2E8F0] bg-white p-3">
      <p className="text-[10px] font-mono uppercase tracking-wider text-[#64748B]">{label}</p>
      <p className="mt-1.5 text-sm font-medium text-[#0F172A]">{value}</p>
    </div>
  )
}

function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-[#0F172A]/45 p-4" onMouseDown={event => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <section role="dialog" aria-modal="true" aria-label={title} className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-[#E2E8F0] bg-white shadow-2xl">
        <header className="border-b border-[#E2E8F0] px-5 py-4">
          <h2 className="text-base font-semibold text-[#0F172A]">{title}</h2>
        </header>
        <div className="p-5">{children}</div>
      </section>
    </div>
  )
}
