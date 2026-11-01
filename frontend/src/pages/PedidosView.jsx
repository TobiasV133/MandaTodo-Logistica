import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import PedidoAccionesModal from '../components/pedidos/PedidoAccionesModal'
import TarifaModal from '../components/tarifas/TarifaModal'
import EmptyState from '../components/ui/EmptyState'
import EstadoBadge from '../components/ui/EstadoBadge'
import Icon from '../components/ui/Icon'
import Pagination from '../components/ui/Pagination'
import { ESTADOS_LISTA } from '../constants/estados'
import { useAuth } from '../context/AuthContext'
import { getApiErrorMessage } from '../services/api'
import { listarPedidos } from '../services/pedidoService'
import { formatFechaHora, formatMoneda, formatPeso } from '../utils/formatters'

const FILTROS = [{ value: 'TODOS', label: 'Todos' }, ...ESTADOS_LISTA]
const VALORES_FILTRO = new Set(FILTROS.map((item) => item.value))
const PAGE_SIZE = 5

function normalizarFiltro(valor) {
  const estado = typeof valor === 'string' ? valor.trim().toUpperCase() : ''
  return VALORES_FILTRO.has(estado) ? estado : 'TODOS'
}

export default function PedidosView({ soloRevision = false }) {
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const filtroUrl = normalizarFiltro(searchParams.get('estado'))
  const [filtro, setFiltro] = useState(filtroUrl)
  const [pedidos, setPedidos] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pedidoSeleccionado, setPedidoSeleccionado] = useState(null)
  const [mostrarTarifas, setMostrarTarifas] = useState(false)
  const [pagina, setPagina] = useState(1)

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFiltro((prev) => (prev === filtroUrl ? prev : filtroUrl))
    setPagina(1)
  }, [filtroUrl])

  const estadoConsulta = soloRevision ? 'EN_REVISION' : filtro

  const cambiarFiltro = useCallback(
    (nuevoFiltro) => {
      const filtroNormalizado = normalizarFiltro(nuevoFiltro)
      const siguientes = new URLSearchParams(searchParams)

      setFiltro(filtroNormalizado)
      setPagina(1)

      if (filtroNormalizado === 'TODOS') {
        siguientes.delete('estado')
      } else {
        siguientes.set('estado', filtroNormalizado)
      }

      setSearchParams(siguientes)
    },
    [searchParams, setSearchParams],
  )

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

  const totalPaginas = Math.max(1, Math.ceil(pedidosFiltrados.length / PAGE_SIZE))
  const paginaActual = Math.min(pagina, totalPaginas)
  const inicio = (paginaActual - 1) * PAGE_SIZE
  const pedidosPagina = pedidosFiltrados.slice(inicio, inicio + PAGE_SIZE)

  return (
    <div className="page-stack page-stack--fill">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">
            {soloRevision ? 'Bandeja de retención' : 'Gestión de envíos'}
          </span>
          <h2>{soloRevision ? 'Pedidos en revisión' : 'Pedidos'}</h2>
          <p>
            {soloRevision
              ? 'Cargas retenidas por dirección ambigua o por posible duplicado de local.'
              : 'Todas las cargas registradas en MandáTodo, ordenadas por fecha.'}
          </p>
        </div>
        <div className="page-heading__acciones">
          {!soloRevision && isAdmin && (
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              onClick={() => setMostrarTarifas(true)}
            >
              <Icon name="money" size={16} />
              Tarifas
            </button>
          )}
          <button type="button" className="btn btn-outline-secondary btn-sm" onClick={cargar} disabled={loading}>
            <Icon name="refresh" size={16} />
            Actualizar
          </button>
        </div>
      </div>

      {soloRevision && (
        <div className="review-rules">
          <span className="review-rules__item">
            <Icon name="mapPin" size={16} /> Dirección ambigua, incompleta o S/N
          </span>
          <span className="review-rules__item">
            <Icon name="building" size={16} /> Empresa con duplicado del día
          </span>
          <span className="review-rules__item">
            <Icon name="user" size={16} /> Particular: solo revisión por dirección
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
              onClick={() => cambiarFiltro(item.value)}
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
              onChange={(event) => {
                setBusqueda(event.target.value)
                setPagina(1)
              }}
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
          <>
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
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {pedidosPagina.map((pedido) => (
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
                          {formatPeso(pedido.pesoKg)} kg
                          {pedido.kilometros != null ? ` · ${formatPeso(pedido.kilometros)} km` : ''}
                        </small>
                      </div>
                    </td>
                    <td>
                      <span className="money-cell">
                        {formatMoneda(pedido.montoEnvio ?? pedido.precioTotal)}
                      </span>
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
                    <td className="text-end">
                      <button
                        type="button"
                        className={`btn btn-sm ${
                          soloRevision ? 'btn-primary' : 'btn-outline-primary'
                        }`}
                        onClick={() => setPedidoSeleccionado(pedido)}
                      >
                        <Icon name={soloRevision ? 'checkCircle' : 'truck'} size={15} />
                        {soloRevision ? 'Revisar' : 'Gestionar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
            <Pagination
              page={paginaActual}
              totalItems={pedidosFiltrados.length}
              pageSize={PAGE_SIZE}
              onPageChange={setPagina}
              ariaLabel="Paginación de pedidos"
            />
          </>
        )}
      </div>

      {pedidoSeleccionado && (
        <PedidoAccionesModal
          key={pedidoSeleccionado.idPedido}
          pedido={pedidoSeleccionado}
          onClose={() => setPedidoSeleccionado(null)}
          onUpdated={cargar}
        />
      )}

      {mostrarTarifas && <TarifaModal onClose={() => setMostrarTarifas(false)} />}
    </div>
  )
}
