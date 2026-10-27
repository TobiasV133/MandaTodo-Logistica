import { useCallback, useEffect, useMemo, useState } from 'react'

import OperarioFormModal from '../components/operarios/OperarioFormModal'
import EmptyState from '../components/ui/EmptyState'
import Icon from '../components/ui/Icon'
import { useAuth } from '../context/AuthContext'
import { getApiErrorMessage } from '../services/api'
import {
  cambiarEstadoOperario,
  listarOperarios,
} from '../services/operarioService'
import { iniciales } from '../utils/formatters'

function rolLabel(rol) {
  return rol === 'ADMINISTRADOR' ? 'ADMIN' : 'OPERARIO'
}

function rolBadge(rol) {
  return rol === 'ADMINISTRADOR' ? 'bg-primary' : 'bg-info text-dark'
}

export default function OperariosPage() {
  const { operario: operarioActual } = useAuth()
  const [operarios, setOperarios] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [accionError, setAccionError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [modalAbierto, setModalAbierto] = useState(false)
  const [operarioEditando, setOperarioEditando] = useState(null)
  const [togglingId, setTogglingId] = useState(null)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    setAccionError('')

    try {
      const data = await listarOperarios()
      setOperarios(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudieron cargar los operarios.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    cargar()
  }, [cargar])

  const operariosFiltrados = useMemo(() => {
    const termino = busqueda.trim().toLowerCase()
    if (!termino) return operarios

    return operarios.filter((operario) => {
      const texto = [
        operario.username,
        operario.nombre,
        operario.email,
        rolLabel(operario.rol),
        operario.estado,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      return texto.includes(termino)
    })
  }, [operarios, busqueda])

  const abrirNuevo = () => {
    setOperarioEditando(null)
    setModalAbierto(true)
    setMensaje('')
  }

  const abrirEditar = (operario) => {
    setOperarioEditando(operario)
    setModalAbierto(true)
    setMensaje('')
  }

  const handleSaved = (_data, modo) => {
    setModalAbierto(false)
    setOperarioEditando(null)
    setMensaje(
      modo === 'crear'
        ? 'Operario creado correctamente.'
        : 'Operario actualizado correctamente.',
    )
    cargar()
  }

  const alternarEstado = async (operario) => {
    const nuevoEstado = operario.estado === 'Activo' ? 'Inactivo' : 'Activo'
    setTogglingId(operario.idOperario)
    setAccionError('')
    setMensaje('')

    try {
      await cambiarEstadoOperario(operario, nuevoEstado, operarioActual?.idOperario)
      setMensaje(
        `Operario ${operario.nombre} ${
          nuevoEstado === 'Activo' ? 'activado' : 'desactivado'
        } correctamente.`,
      )
      await cargar()
    } catch (err) {
      setAccionError(
        getApiErrorMessage(err, 'No se pudo cambiar el estado del operario.'),
      )
    } finally {
      setTogglingId(null)
    }
  }

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Seguridad</span>
          <h2>Gestión de operarios</h2>
          <p>Altas, edición de roles y control de acceso al sistema.</p>
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
            Nuevo operario
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

      {accionError && (
        <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
          <Icon name="alert" size={18} />
          <span>{accionError}</span>
          <button
            type="button"
            className="btn-close ms-auto"
            aria-label="Cerrar"
            onClick={() => setAccionError('')}
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
            <strong>{operariosFiltrados.length}</strong>
            <span>{operariosFiltrados.length === 1 ? 'operario' : 'operarios'}</span>
          </div>

          <div className="input-with-icon table-search">
            <Icon name="search" size={17} />
            <input
              type="search"
              className="form-control"
              placeholder="Buscar por usuario, nombre, email o rol"
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              aria-label="Buscar operarios"
            />
          </div>
        </div>

        {loading ? (
          <div className="table-loading">
            <span className="spinner-border text-primary" role="status" aria-hidden="true" />
            <span>Cargando operarios…</span>
          </div>
        ) : operariosFiltrados.length === 0 ? (
          <EmptyState
            icon="shield"
            title={operarios.length === 0 ? 'Todavía no hay operarios' : 'No encontramos coincidencias'}
            description={
              operarios.length === 0
                ? 'Creá el primer operario para habilitar el acceso al sistema.'
                : 'Probá con otro nombre, email o rol.'
            }
            action={
              operarios.length === 0 ? (
                <button type="button" className="btn btn-primary" onClick={abrirNuevo}>
                  <Icon name="plus" size={17} className="me-1" />
                  Nuevo operario
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setBusqueda('')}
                >
                  Limpiar búsqueda
                </button>
              )
            }
          />
        ) : (
          <div className="table-responsive">
            <table className="table table-mt align-middle">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Usuario</th>
                  <th>Nombre completo</th>
                  <th>Email</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {operariosFiltrados.map((operario) => {
                  const esMiUsuario = operario.idOperario === operarioActual?.idOperario
                  const estaActivo = operario.estado === 'Activo'
                  const cambiando = togglingId === operario.idOperario

                  return (
                    <tr key={operario.idOperario}>
                      <td>
                        <span className="code-cell">#{operario.idOperario}</span>
                      </td>

                      <td>
                        <span className="code-cell">@{operario.username}</span>
                      </td>

                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <span className="avatar">{iniciales(operario.nombre)}</span>
                          <div className="client-cell">
                            <strong>{operario.nombre}</strong>
                            <span>{esMiUsuario ? 'Tu usuario' : 'Operario del sistema'}</span>
                          </div>
                        </div>
                      </td>

                      <td>
                        <div className="contact-cell">
                          <span>{operario.email}</span>
                        </div>
                      </td>

                      <td>
                        <span className={`badge ${rolBadge(operario.rol)}`}>
                          {rolLabel(operario.rol)}
                        </span>
                      </td>

                      <td>
                        <span className={`badge ${estaActivo ? 'bg-success' : 'bg-secondary'}`}>
                          {operario.estado}
                        </span>
                      </td>

                      <td>
                        <div className="actions-cell justify-content-end">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-primary"
                            onClick={() => abrirEditar(operario)}
                          >
                            <Icon name="edit" size={15} className="me-1" />
                            Editar
                          </button>

                          <button
                            type="button"
                            className={`btn btn-sm ${
                              estaActivo ? 'btn-outline-danger' : 'btn-outline-success'
                            }`}
                            onClick={() => alternarEstado(operario)}
                            disabled={cambiando || esMiUsuario}
                            title={
                              esMiUsuario
                                ? 'No podés cambiar tu propio estado'
                                : estaActivo
                                  ? 'Desactivar operario'
                                  : 'Activar operario'
                            }
                          >
                            {cambiando ? (
                              <span
                                className="spinner-border spinner-border-sm me-1"
                                aria-hidden="true"
                              />
                            ) : (
                              <Icon
                                name={estaActivo ? 'toggleRight' : 'toggleLeft'}
                                size={16}
                                className="me-1"
                              />
                            )}
                            {estaActivo ? 'Desactivar' : 'Activar'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalAbierto && (
        <OperarioFormModal
          key={operarioEditando?.idOperario ?? 'nuevo'}
          operario={operarioEditando}
          onClose={() => setModalAbierto(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  )
}
