import { useCallback, useEffect, useMemo, useState } from 'react'

import EmptyState from '../components/ui/EmptyState'
import Icon from '../components/ui/Icon'
import { getApiErrorMessage } from '../services/api'
import { listarAuditoria } from '../services/auditoriaService'
import { listarOperarios } from '../services/operarioService'
import { formatFechaHora } from '../utils/formatters'

const PAGE_SIZE = 10

function badgeTipoEvento(tipoEvento) {
  switch (tipoEvento) {
    case 'Alta':
      return 'bg-success'
    case 'Edición':
      return 'bg-primary'
    case 'Reimpresión':
      return 'bg-info text-dark'
    case 'Cambio de Estado':
      return 'bg-warning text-dark'
    default:
      return 'bg-secondary'
  }
}

function moduloEvento(actividad) {
  const texto = String(actividad ?? '').toLowerCase()
  if (texto.includes('pedido')) return 'Pedido'
  if (texto.includes('cliente')) return 'Cliente'
  if (texto.includes('operario')) return 'Operario'
  return 'Sistema'
}

function normalizar(valor) {
  return String(valor ?? '').toLowerCase()
}

export default function AuditoriaPage() {
  const [eventos, setEventos] = useState([])
  const [operarios, setOperarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [fechaDesde, setFechaDesde] = useState('')
  const [fechaHasta, setFechaHasta] = useState('')
  const [tipo, setTipo] = useState('')
  const [operarioId, setOperarioId] = useState('')
  const [texto, setTexto] = useState('')
  const [pagina, setPagina] = useState(1)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const [auditoriaData, operariosData] = await Promise.all([
        listarAuditoria(),
        listarOperarios().catch(() => []),
      ])

      setEventos(Array.isArray(auditoriaData) ? auditoriaData : [])
      setOperarios(Array.isArray(operariosData) ? operariosData : [])
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'No se pudo cargar el historial de auditoría.'),
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    cargar()
  }, [cargar])

  const operariosPorId = useMemo(
    () => new Map(operarios.map((operario) => [operario.idOperario, operario])),
    [operarios],
  )

  const eventosFiltrados = useMemo(() => {
    const desde = fechaDesde ? new Date(`${fechaDesde}T00:00:00`) : null
    const hasta = fechaHasta ? new Date(`${fechaHasta}T23:59:59.999`) : null
    const termino = texto.trim().toLowerCase()
    const operarioFiltro = operarioId ? Number(operarioId) : null

    return eventos
      .filter((evento) => {
        const fechaEvento = new Date(evento.fechaHora)
        if (desde && fechaEvento < desde) return false
        if (hasta && fechaEvento > hasta) return false
        if (operarioFiltro && evento.idOperario !== operarioFiltro) return false

        const actividad = normalizar(evento.actividad)

        if (tipo === 'ALTA' && evento.tipoEvento !== 'Alta') return false
        if (tipo === 'EDICION' && evento.tipoEvento !== 'Edición') return false
        if (tipo === 'REIMPRESION' && evento.tipoEvento !== 'Reimpresión') return false
        if (tipo === 'CAMBIO_ESTADO' && evento.tipoEvento !== 'Cambio de Estado') return false
        if (tipo === 'ELIMINACION' && !actividad.includes('elimin')) return false
        if (tipo === 'CLIENTE' && !actividad.includes('cliente')) return false
        if (tipo === 'OPERARIO' && !actividad.includes('operario')) return false
        if (tipo === 'PEDIDO' && !actividad.includes('pedido')) return false

        if (termino) {
          const nombreOperario =
            operariosPorId.get(evento.idOperario)?.nombre ?? ''
          const textoEvento = [
            evento.actividad,
            evento.tipoEvento,
            evento.idPedido,
            nombreOperario,
          ]
            .filter(Boolean)
            .join(' ')
            .toLowerCase()

          if (!textoEvento.includes(termino)) return false
        }

        return true
      })
      .sort((a, b) => new Date(b.fechaHora) - new Date(a.fechaHora))
  }, [eventos, operariosPorId, fechaDesde, fechaHasta, tipo, operarioId, texto])

  const totalPaginas = Math.max(1, Math.ceil(eventosFiltrados.length / PAGE_SIZE))
  const paginaActual = Math.min(pagina, totalPaginas)
  const inicio = (paginaActual - 1) * PAGE_SIZE
  const eventosPagina = eventosFiltrados.slice(inicio, inicio + PAGE_SIZE)
  const hayFiltros = Boolean(fechaDesde || fechaHasta || tipo || operarioId || texto)

  const actualizarFechaDesde = (value) => {
    setFechaDesde(value)
    setPagina(1)
  }

  const actualizarFechaHasta = (value) => {
    setFechaHasta(value)
    setPagina(1)
  }

  const actualizarTipo = (value) => {
    setTipo(value)
    setPagina(1)
  }

  const actualizarOperario = (value) => {
    setOperarioId(value)
    setPagina(1)
  }

  const actualizarTexto = (value) => {
    setTexto(value)
    setPagina(1)
  }

  const limpiarFiltros = () => {
    setFechaDesde('')
    setFechaHasta('')
    setTipo('')
    setOperarioId('')
    setTexto('')
    setPagina(1)
  }

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Trazabilidad</span>
          <h2>Auditoría global</h2>
          <p>Registro cronológico e inalterable de las acciones del sistema.</p>
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          onClick={cargar}
          disabled={loading}
        >
          <Icon name="refresh" size={16} className="me-1" />
          Actualizar
        </button>
      </div>

      <div className="alert alert-info d-flex align-items-start gap-2" role="status">
        <Icon name="lock" size={18} className="mt-1 flex-shrink-0" />
        <div>
          <strong>Registro inalterable.</strong> La auditoría es append-only: los
          eventos no se pueden editar ni eliminar.
        </div>
      </div>

      {error && (
        <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
          <Icon name="alert" size={18} />
          <span>{error}</span>
          <button type="button" className="btn btn-sm btn-outline-danger ms-auto" onClick={cargar}>
            Reintentar
          </button>
        </div>
      )}

      <div className="panel p-3">
        <div className="row g-3 align-items-end">
          <div className="col-12 col-sm-6 col-xl-2">
            <label className="form-label field-label" htmlFor="auditoria-desde">
              Fecha desde
            </label>
            <input
              id="auditoria-desde"
              type="date"
              className="form-control"
              value={fechaDesde}
              max={fechaHasta || undefined}
              onChange={(event) => actualizarFechaDesde(event.target.value)}
            />
          </div>

          <div className="col-12 col-sm-6 col-xl-2">
            <label className="form-label field-label" htmlFor="auditoria-hasta">
              Fecha hasta
            </label>
            <input
              id="auditoria-hasta"
              type="date"
              className="form-control"
              value={fechaHasta}
              min={fechaDesde || undefined}
              onChange={(event) => actualizarFechaHasta(event.target.value)}
            />
          </div>

          <div className="col-12 col-sm-6 col-xl-2">
            <label className="form-label field-label" htmlFor="auditoria-tipo">
              Tipo / entidad
            </label>
            <select
              id="auditoria-tipo"
              className="form-select"
              value={tipo}
              onChange={(event) => actualizarTipo(event.target.value)}
            >
              <option value="">Todos los eventos</option>
              <option value="ALTA">Alta (INSERT)</option>
              <option value="EDICION">Edición (UPDATE)</option>
              <option value="REIMPRESION">Reimpresión</option>
              <option value="CAMBIO_ESTADO">Cambio de estado</option>
              <option value="ELIMINACION">Eliminación (no permitida)</option>
              <option value="CLIENTE">Clientes</option>
              <option value="OPERARIO">Operarios</option>
              <option value="PEDIDO">Pedidos</option>
            </select>
          </div>

          <div className="col-12 col-sm-6 col-xl-3">
            <label className="form-label field-label" htmlFor="auditoria-operario">
              Operario responsable
            </label>
            <select
              id="auditoria-operario"
              className="form-select"
              value={operarioId}
              onChange={(event) => actualizarOperario(event.target.value)}
            >
              <option value="">Todos los operarios</option>
              {operarios.map((operario) => (
                <option key={operario.idOperario} value={operario.idOperario}>
                  {operario.nombre}
                </option>
              ))}
            </select>
          </div>

          <div className="col-12 col-xl-3">
            <label className="form-label field-label" htmlFor="auditoria-texto">
              Buscar
            </label>
            <div className="input-with-icon">
              <Icon name="search" size={17} />
              <input
                id="auditoria-texto"
                type="search"
                className="form-control"
                value={texto}
                onChange={(event) => actualizarTexto(event.target.value)}
                placeholder="Detalle, operario o pedido"
              />
            </div>
          </div>

          <div className="col-12 d-flex justify-content-end">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={limpiarFiltros}
              disabled={!hayFiltros}
            >
              Limpiar filtros
            </button>
          </div>
        </div>
      </div>

      <div className="panel panel--table">
        <div className="table-toolbar">
          <div className="table-toolbar__title">
            <strong>{eventosFiltrados.length}</strong>
            <span>{eventosFiltrados.length === 1 ? 'evento' : 'eventos'}</span>
          </div>
          {hayFiltros && (
            <span className="badge bg-light text-secondary border">
              Filtros activos
            </span>
          )}
        </div>

        {loading ? (
          <div className="table-loading">
            <span className="spinner-border text-primary" role="status" aria-hidden="true" />
            <span>Cargando auditoría…</span>
          </div>
        ) : eventosFiltrados.length === 0 ? (
          <EmptyState
            icon="history"
            title={eventos.length === 0 ? 'Todavía no hay eventos' : 'No encontramos eventos'}
            description={
              eventos.length === 0
                ? 'Las acciones del sistema aparecerán aquí a medida que se registren.'
                : 'Probá ajustando el rango de fechas, el operario o el tipo de evento.'
            }
            action={
              hayFiltros ? (
                <button type="button" className="btn btn-outline-secondary" onClick={limpiarFiltros}>
                  Limpiar filtros
                </button>
              ) : null
            }
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="table table-mt align-middle">
                <thead>
                  <tr>
                    <th>Fecha / hora</th>
                    <th>Operario</th>
                    <th>Acción / módulo</th>
                    <th>Detalle</th>
                    <th>Pedido</th>
                  </tr>
                </thead>
                <tbody>
                  {eventosPagina.map((evento) => {
                    const operario = operariosPorId.get(evento.idOperario)

                    return (
                      <tr key={evento.idAuditoria}>
                        <td>
                          <span className="date-cell">{formatFechaHora(evento.fechaHora)}</span>
                        </td>

                        <td>
                          <div className="audit-operator">
                            <strong>{operario?.nombre ?? `Operario #${evento.idOperario}`}</strong>
                            <small>{operario?.email ?? 'Sin datos de contacto'}</small>
                          </div>
                        </td>

                        <td>
                          <div className="audit-badges">
                            <span className={`badge ${badgeTipoEvento(evento.tipoEvento)}`}>
                              {evento.tipoEvento}
                            </span>
                            <span className="badge bg-light text-secondary border">
                              {moduloEvento(evento.actividad)}
                            </span>
                          </div>
                        </td>

                        <td>
                          <span className="audit-detail">{evento.actividad}</span>
                        </td>

                        <td>
                          {evento.idPedido ? (
                            <span className="code-cell">#{evento.idPedido}</span>
                          ) : (
                            <span className="text-secondary">—</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="audit-pagination">
              <span className="text-secondary small">
                Mostrando {inicio + 1}–{Math.min(inicio + PAGE_SIZE, eventosFiltrados.length)} de{' '}
                {eventosFiltrados.length}
              </span>

              <nav aria-label="Paginación de auditoría">
                <ul className="pagination pagination-sm mb-0">
                  <li className={`page-item ${paginaActual <= 1 ? 'disabled' : ''}`}>
                    <button
                      type="button"
                      className="page-link"
                      onClick={() => setPagina(Math.max(1, paginaActual - 1))}
                      disabled={paginaActual <= 1}
                    >
                      Anterior
                    </button>
                  </li>
                  <li className="page-item disabled">
                    <span className="page-link">
                      Página {paginaActual} de {totalPaginas}
                    </span>
                  </li>
                  <li className={`page-item ${paginaActual >= totalPaginas ? 'disabled' : ''}`}>
                    <button
                      type="button"
                      className="page-link"
                      onClick={() => setPagina(Math.min(totalPaginas, paginaActual + 1))}
                      disabled={paginaActual >= totalPaginas}
                    >
                      Siguiente
                    </button>
                  </li>
                </ul>
              </nav>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
