import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import EmptyState from '../components/ui/EmptyState'
import EstadoBadge from '../components/ui/EstadoBadge'
import Icon from '../components/ui/Icon'
import { ESTADOS_LISTA } from '../constants/estados'
import { getApiErrorMessage } from '../services/api'
import { listarPedidos } from '../services/pedidoService'
import { formatFechaHora, formatMoneda, formatPeso } from '../utils/formatters'

const FILTROS = [{ value: 'TODOS', label: 'Todos' }, ...ESTADOS_LISTA]

export default function PedidosView({ soloRevision = false }) {
  const navigate = useNavigate()
  const [pedidos, setPedidos] = useState([])
  const [filtro, setFiltro] = useState('TODOS')
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const estadoConsulta = soloRevision ? 'EN_REVISION' : filtro

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await listarPedidos(estadoConsulta === 'TODOS' ? undefined : estadoConsulta)
      setPedidos(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudieron cargar los pedidos.'))
    } finally {
      setLoading(false)
    }
  }, [estadoConsulta])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    cargar()
  }, [cargar])

  const pedidosFiltrados = useMemo(() => {
    const termino = busqueda.trim().toLowerCase()
    if (!termino) return pedidos

    return pedidos.filter((pedido) => {
      const texto = [
        pedido.codigoPedido,
        pedido.nombreCliente,
        pedido.numeroDocumento,
        pedido.direccionCompleta,
        pedido.descripcionPaquete,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return texto.includes(termino)
    })
  }, [pedidos, busqueda])

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">
            {soloRevision ? 'Bandeja de retención' : 'Gestión de envíos'}
          </span>
          <h2>{soloRevision ? 'Pedidos en revisión' : 'Pedidos'}</h2>
          <p>
            {soloRevision
              ? 'Cargas retenidas por dirección ambigua o por posible duplicado comercial.'
              : 'Todas las cargas registradas en MandáTodo, ordenadas por fecha.'}
          </p>
        </div>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={cargar} disabled={loading}>
          <Icon name="refresh" size={16} />
          Actualizar
        </button>
      </div>

      {soloRevision && (
        <div className="review-rules">
          <span className="review-rules__item">
            <Icon name="mapPin" size={16} /> Dirección ambigua, incompleta o S/N
          </span>
          <span className="review-rules__item">
            <Icon name="building" size={16} /> Empresa no frecuente con duplicado del día
          </span>
          <span className="review-rules__item review-rules__item--ok">
            <Icon name="star" size={16} /> Cliente frecuente: siempre pasa a Pendiente
          </span>
        </div>
      )}

      {!soloRevision && (
        <div className="filter-row">
          {FILTROS.map((item) => (
            <button
              key={item.value}
              type="button"
              className={`filter-pill ${filtro === item.value ? 'active' : ''}`}
              onClick={() => setFiltro(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}

      <div className="panel panel--table">
        <div className="table-toolbar">
          <div className="table-toolbar__title">
            <strong>{pedidosFiltrados.length}</strong>
            <span>{pedidosFiltrados.length === 1 ? 'pedido' : 'pedidos'}</span>
          </div>
          <div className="input-with-icon table-search">
            <Icon name="search" size={17} />
            <input
              type="search"
              className="form-control"
              placeholder="Buscar por código, cliente o dirección"
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              aria-label="Buscar pedidos"
            />
          </div>
        </div>

        {error && (
          <div className="alert alert-danger d-flex align-items-center gap-2 m-3" role="alert">
            <Icon name="alert" size={18} />
            <span>{error}</span>
            <button type="button" className="btn btn-sm btn-outline-danger ms-auto" onClick={cargar}>
              Reintentar
            </button>
          </div>
        )}

        {loading ? (
          <div className="table-loading">
            <span className="spinner-border text-primary" role="status" aria-hidden="true" />
            <span>Cargando pedidos…</span>
          </div>
        ) : pedidosFiltrados.length === 0 ? (
          <EmptyState
            icon={soloRevision ? 'checkCircle' : 'package'}
            title={soloRevision ? 'No hay pedidos en revisión' : 'Todavía no hay pedidos'}
            description={
              soloRevision
                ? 'La bandeja está al día. Las cargas con dirección validada pasan directo a Pendiente.'
                : 'Registrá la primera carga para comenzar a operar con MandáTodo.'
            }
            action={
              <button type="button" className="btn btn-primary" onClick={() => navigate('/nueva-carga')}>
                <Icon name="plus" size={17} />
                Nueva carga
              </button>
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="table table-mt align-middle">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Cliente</th>
                  <th>Dirección</th>
                  <th>Paquete</th>
                  <th>Tarifa</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {pedidosFiltrados.map((pedido) => (
                  <tr key={pedido.idPedido}>
                    <td>
                      <span className="code-cell">{pedido.codigoPedido}</span>
                    </td>
                    <td>
                      <div className="client-cell">
                        <strong>{pedido.nombreCliente}</strong>
                        <span>
                          {pedido.numeroDocumento}
                          {pedido.clienteFrecuente ? ' · Frecuente' : ''}
                        </span>
                      </div>
                    </td>
                    <td>
                      <div className="address-cell" title={pedido.direccionCompleta}>
                        {pedido.direccionCompleta}
                      </div>
                    </td>
                    <td>
                      <div className="package-cell">
                        <span title={pedido.descripcionPaquete}>{pedido.descripcionPaquete}</span>
                        <small>
                          {formatPeso(pedido.pesoKg)} kg · {formatPeso(pedido.kilometros)} km
                        </small>
                      </div>
                    </td>
                    <td>
                      <span className="money-cell">{formatMoneda(pedido.precioTotal)}</span>
                    </td>
                    <td>
                      <EstadoBadge estado={pedido.estado} />
                      {pedido.estado === 'EN_REVISION' && (
                        <div className="row-note" title={pedido.motivoRevision || undefined}>
                          {pedido.esDireccionIncompleta
                            ? 'Dirección incompleta'
                            : pedido.motivoRevision || 'Revisión manual'}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="date-cell">{formatFechaHora(pedido.fechaCreacion)}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
