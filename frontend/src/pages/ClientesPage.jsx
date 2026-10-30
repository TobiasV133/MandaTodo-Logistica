import { useCallback, useEffect, useMemo, useState } from 'react'

import ClienteFormModal from '../components/clientes/ClienteFormModal'
import EmptyState from '../components/ui/EmptyState'
import Icon from '../components/ui/Icon'
import Pagination from '../components/ui/Pagination'
import { useAuth } from '../context/AuthContext'
import { getApiErrorMessage } from '../services/api'
import { listarClientes } from '../services/clienteService'
import { formatearDocumento } from '../utils/documentos'
import { formatFecha } from '../utils/formatters'

function badgeTipoCliente(tipoCliente) {
  return tipoCliente === 'Empresa' ? 'bg-primary' : 'bg-success'
}

const PAGE_SIZE = 10

export default function ClientesPage() {
  const { operario } = useAuth()
  const [clientes, setClientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [modalAbierto, setModalAbierto] = useState(false)
  const [clienteEditando, setClienteEditando] = useState(null)
  const [pagina, setPagina] = useState(1)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const data = await listarClientes()
      setClientes(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudieron cargar los clientes.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    cargar()
  }, [cargar])

  const clientesFiltrados = useMemo(() => {
    const termino = busqueda.trim().toLowerCase()
    if (!termino) return clientes

    return clientes.filter((cliente) => {
      const texto = [
        cliente.nombreRazonSocial,
        cliente.tipoDocumento,
        cliente.numeroDocumento,
        cliente.email,
        cliente.telefono,
        cliente.tipoCliente,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      return texto.includes(termino)
    })
  }, [clientes, busqueda])

  const totalPaginas = Math.max(1, Math.ceil(clientesFiltrados.length / PAGE_SIZE))
  const paginaActual = Math.min(pagina, totalPaginas)
  const inicio = (paginaActual - 1) * PAGE_SIZE
  const clientesPagina = clientesFiltrados.slice(inicio, inicio + PAGE_SIZE)

  const abrirNuevo = () => {
    setClienteEditando(null)
    setModalAbierto(true)
    setMensaje('')
  }

  const abrirEditar = (cliente) => {
    setClienteEditando(cliente)
    setModalAbierto(true)
    setMensaje('')
  }

  const handleSaved = (_data, modo) => {
    setModalAbierto(false)
    setClienteEditando(null)
    setMensaje(
      modo === 'crear'
        ? 'Cliente creado correctamente.'
        : 'Cliente actualizado correctamente.',
    )
    cargar()
  }

  return (
    <div className="page-stack page-stack--fill">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Base comercial</span>
          <h2>Clientes</h2>
          <p>Personas y empresas que ya operaron con MandáTodo.</p>
        </div>
        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={cargar}
            disabled={loading}
          >
            <Icon name="refresh" size={16} className="me-1" />
            Actualizar
          </button>
          <button type="button" className="btn btn-primary btn-sm" onClick={abrirNuevo}>
            <Icon name="plus" size={16} className="me-1" />
            Nuevo cliente
          </button>
        </div>
      </div>

      {mensaje && (
        <div className="alert alert-success d-flex align-items-center gap-2" role="status">
          <Icon name="checkCircle" size={18} />
          <span>{mensaje}</span>
          <button
            type="button"
            className="btn-close ms-auto"
            aria-label="Cerrar"
            onClick={() => setMensaje('')}
          />
        </div>
      )}

      {error && (
        <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
          <Icon name="alert" size={18} />
          <span>{error}</span>
          <button type="button" className="btn btn-sm btn-outline-danger ms-auto" onClick={cargar}>
            Reintentar
          </button>
        </div>
      )}

      <div className="panel panel--table">
        <div className="table-toolbar">
          <div className="table-toolbar__title">
            <strong>{clientesFiltrados.length}</strong>
            <span>{clientesFiltrados.length === 1 ? 'cliente' : 'clientes'}</span>
          </div>

          <div className="input-with-icon table-search">
            <Icon name="search" size={17} />
            <input
              type="search"
              className="form-control"
              placeholder="Buscar por nombre, DNI/CUIT o email"
              value={busqueda}
              onChange={(event) => {
                setBusqueda(event.target.value)
                setPagina(1)
              }}
              aria-label="Buscar clientes"
            />
          </div>
        </div>

        {loading ? (
          <div className="table-loading">
            <span className="spinner-border text-primary" role="status" aria-hidden="true" />
            <span>Cargando clientes…</span>
          </div>
        ) : clientesFiltrados.length === 0 ? (
          <EmptyState
            icon="users"
            title={clientes.length === 0 ? 'Todavía no hay clientes' : 'No encontramos coincidencias'}
            description={
              clientes.length === 0
                ? 'Registrá el primer cliente para empezar a operar.'
                : 'Probá con otro nombre, documento o email.'
            }
            action={
              clientes.length === 0 ? (
                <button type="button" className="btn btn-primary" onClick={abrirNuevo}>
                  <Icon name="plus" size={17} className="me-1" />
                  Nuevo cliente
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => {
                    setBusqueda('')
                    setPagina(1)
                  }}
                >
                  Limpiar búsqueda
                </button>
              )
            }
          />
        ) : (
          <>
            <div className="table-responsive">
              <table className="table table-mt align-middle">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Documento</th>
                  <th>Contacto</th>
                  <th>Tipo</th>
                  <th>Condición</th>
                  <th>Alta</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {clientesPagina.map((cliente) => (
                  <tr key={cliente.idCliente}>
                    <td>
                      <div className="client-cell">
                        <strong>{cliente.nombreRazonSocial}</strong>
                        <span>{cliente.tipoCliente}</span>
                      </div>
                    </td>

                    <td>
                      <span className="code-cell">
                        {cliente.tipoDocumento}{' '}
                        {formatearDocumento(cliente.tipoDocumento, cliente.numeroDocumento)}
                      </span>
                    </td>

                    <td>
                      <div className="contact-cell">
                        <span>{cliente.email}</span>
                        <small>{cliente.telefono || 'Sin teléfono'}</small>
                      </div>
                    </td>

                    <td>
                      <span className={`badge ${badgeTipoCliente(cliente.tipoCliente)}`}>
                        {cliente.tipoCliente}
                      </span>
                    </td>

                    <td>
                      {cliente.esFrecuente ? (
                        <span className="badge bg-warning text-dark">
                          <Icon name="star" size={12} className="me-1" />
                          Frecuente
                        </span>
                      ) : (
                        <span className="badge bg-light text-secondary border">Ocasional</span>
                      )}
                    </td>

                    <td>
                      <span className="date-cell">{formatFecha(cliente.fechaCreacion)}</span>
                    </td>

                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => abrirEditar(cliente)}
                      >
                        <Icon name="edit" size={15} className="me-1" />
                        Editar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
            <Pagination
              page={paginaActual}
              totalItems={clientesFiltrados.length}
              pageSize={PAGE_SIZE}
              onPageChange={setPagina}
              ariaLabel="Paginación de clientes"
            />
          </>
        )}
      </div>

      {modalAbierto && (
        <ClienteFormModal
          key={clienteEditando?.idCliente ?? 'nuevo'}
          cliente={clienteEditando}
          onClose={() => setModalAbierto(false)}
          onSaved={handleSaved}
        />
      )}

      <div className="text-secondary small">
        Operador actual: {operario?.nombreCompleto ?? operario?.nombre ?? 'sin sesión'}
      </div>
    </div>
  )
}
