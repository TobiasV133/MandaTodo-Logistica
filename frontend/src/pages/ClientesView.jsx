import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import EmptyState from '../components/ui/EmptyState'
import Icon from '../components/ui/Icon'
import { getApiErrorMessage } from '../services/api'
import { listarClientes } from '../services/clienteService'
import { formatearDocumento } from '../utils/documentos'
import { formatFecha } from '../utils/formatters'

export default function ClientesView() {
  const navigate = useNavigate()
  const [clientes, setClientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

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

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Base comercial</span>
          <h2>Clientes</h2>
          <p>Personas y empresas que ya operaron con MandáTodo.</p>
        </div>
        <div className="d-flex gap-2">
          <button type="button" className="btn btn-outline-secondary btn-sm" onClick={cargar} disabled={loading}>
            <Icon name="refresh" size={16} />
            Actualizar
          </button>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/nueva-carga')}>
            <Icon name="plus" size={16} />
            Nueva carga
          </button>
        </div>
      </div>

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
              placeholder="Buscar por nombre, documento o email"
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              aria-label="Buscar clientes"
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
            <span>Cargando clientes…</span>
          </div>
        ) : clientesFiltrados.length === 0 ? (
          <EmptyState
            icon="users"
            title="No hay clientes para mostrar"
            description="Los clientes se crean automáticamente al registrar la primera carga con su documento."
            action={
              <button type="button" className="btn btn-primary" onClick={() => navigate('/nueva-carga')}>
                <Icon name="plus" size={17} />
                Registrar pedido
              </button>
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="table table-mt align-middle">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Documento</th>
                  <th>Contacto</th>
                  <th>Tipo</th>
                  <th>Frecuente</th>
                  <th>Alta</th>
                </tr>
              </thead>
              <tbody>
                {clientesFiltrados.map((cliente) => (
                  <tr key={cliente.idCliente}>
                    <td>
                      <div className="client-cell">
                        <strong>{cliente.nombreRazonSocial}</strong>
                        <span>{cliente.tipoCliente === 'Empresa' ? 'Empresa' : 'Particular'}</span>
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
                      <span className={`chip ${cliente.tipoCliente === 'Empresa' ? 'chip--blue' : 'chip--neutral'}`}>
                        {cliente.tipoCliente}
                      </span>
                    </td>
                    <td>
                      {cliente.esFrecuente ? (
                        <span className="chip chip--success">
                          <Icon name="star" size={13} />
                          Frecuente
                        </span>
                      ) : (
                        <span className="text-muted">No</span>
                      )}
                    </td>
                    <td>
                      <span className="date-cell">{formatFecha(cliente.fechaCreacion)}</span>
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
