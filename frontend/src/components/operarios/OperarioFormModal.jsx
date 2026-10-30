import { useState } from 'react'

import Field from '../ui/Field'
import Icon from '../ui/Icon'
import Modal from '../ui/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import {
  actualizarOperario,
  crearOperario,
} from '../../services/operarioService'

const ROLES = [
  { value: 'Admin', label: 'ADMIN (Administrador)' },
  { value: 'Operario', label: 'OPERARIO' },
]

const ESTADOS = [
  { value: 'Activo', label: 'Activo' },
  { value: 'Inactivo', label: 'Inactivo' },
]

function formInicial(operario) {
  if (!operario) {
    return {
      username: '',
      nombre: '',
      email: '',
      password: '',
      rol: 'Operario',
      estado: 'Activo',
    }
  }

  return {
    username: operario.username ?? '',
    nombre: operario.nombre ?? '',
    email: operario.email ?? '',
    password: '',
    rol: operario.rol ?? 'Operario',
    estado: operario.estado ?? 'Activo',
  }
}

export default function OperarioFormModal({ operario, onClose, onSaved }) {
  const { operario: operarioActual } = useAuth()
  const editando = Boolean(operario)
  const esMiUsuario = editando && operario.idOperario === operarioActual?.idOperario
  const [form, setForm] = useState(() => formInicial(operario))
  const [errores, setErrores] = useState({})
  const [serverError, setServerError] = useState('')
  const [saving, setSaving] = useState(false)

  const setCampo = (campo) => (event) => {
    setForm((actual) => ({ ...actual, [campo]: event.target.value }))
    setErrores((actual) => ({ ...actual, [campo]: undefined }))
  }

  const cerrar = () => {
    if (!saving) onClose?.()
  }

  const validar = () => {
    const next = {}

    if (!form.username.trim()) {
      next.username = 'El username es obligatorio.'
    } else if (form.username.trim().length < 4 || form.username.trim().length > 30) {
      next.username = 'El username debe tener entre 4 y 30 caracteres.'
    } else if (!/^[a-zA-Z0-9._-]+$/.test(form.username.trim())) {
      next.username =
        'Solo se permiten letras, números, puntos, guiones y guiones bajos.'
    }

    if (!form.nombre.trim()) {
      next.nombre = 'El nombre es obligatorio.'
    } else if (form.nombre.trim().length > 150) {
      next.nombre = 'El nombre no puede superar los 150 caracteres.'
    }

    if (!form.email.trim()) {
      next.email = 'El email es obligatorio.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      next.email = 'Ingresá un email válido.'
    } else if (form.email.trim().length > 150) {
      next.email = 'El email no puede superar los 150 caracteres.'
    }

    if (!editando) {
      if (!form.password) {
        next.password = 'La contraseña es obligatoria para el alta.'
      } else if (form.password.length < 8) {
        next.password = 'La contraseña debe tener al menos 8 caracteres.'
      } else if (form.password.length > 72) {
        next.password = 'La contraseña no puede superar los 72 caracteres.'
      }
    }

    if (!form.rol) {
      next.rol = 'Seleccioná un rol.'
    }

    if (editando && !form.estado) {
      next.estado = 'Seleccioná un estado.'
    }

    setErrores(next)
    return Object.keys(next).length === 0
  }

  const guardar = async (event) => {
    event.preventDefault()
    setServerError('')

    if (!validar()) return

    setSaving(true)

    try {
      if (editando) {
        const data = await actualizarOperario(
          operario.idOperario,
          {
            username: form.username.trim().toLowerCase(),
            nombre: form.nombre.trim(),
            email: form.email.trim().toLowerCase(),
            password: form.password,
            rol: form.rol,
            estado: form.estado,
          },
          operarioActual?.idOperario,
        )
        onSaved?.(data, 'editar')
      } else {
        const data = await crearOperario(
          {
            username: form.username.trim().toLowerCase(),
            nombre: form.nombre.trim(),
            email: form.email.trim().toLowerCase(),
            password: form.password,
            rol: form.rol,
          },
          operarioActual?.idOperario,
        )
        onSaved?.(data, 'crear')
      }
    } catch (err) {
      setServerError(
        getApiErrorMessage(err, 'No se pudo guardar el operario.'),
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={editando ? 'Editar operario' : 'Nuevo operario'}
      size="modal-dialog-scrollable"
      onClose={cerrar}
      footer={
        <>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={cerrar}
            disabled={saving}
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="operario-form"
            className="btn btn-primary"
            disabled={saving}
          >
            {saving ? (
              <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
            ) : (
              <Icon name="check" size={16} className="me-1" />
            )}
            {saving ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear operario'}
          </button>
        </>
      }
    >
      <form id="operario-form" onSubmit={guardar} noValidate>
        {serverError && (
          <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <Icon name="alert" size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <div className="row g-3">
          <div className="col-12">
            <Field
              label="Username"
              htmlFor="operario-username"
              required
              error={errores.username}
              hint="Entre 4 y 30 caracteres. Solo letras, números, puntos, guiones y guiones bajos."
            >
              <input
                id="operario-username"
                className={`form-control ${errores.username ? 'is-invalid' : ''}`}
                value={form.username}
                onChange={setCampo('username')}
                maxLength={30}
                autoComplete="username"
                placeholder="Ej. martin.perez"
              />
            </Field>
          </div>

          <div className="col-12">
            <Field
              label="Nombre completo"
              htmlFor="operario-nombre"
              required
              error={errores.nombre}
            >
              <input
                id="operario-nombre"
                className={`form-control ${errores.nombre ? 'is-invalid' : ''}`}
                value={form.nombre}
                onChange={setCampo('nombre')}
                maxLength={150}
                autoComplete="name"
                placeholder="Ej. Martín Pérez"
              />
            </Field>
          </div>

          <div className="col-12">
            <Field
              label="Email"
              htmlFor="operario-email"
              required
              error={errores.email}
            >
              <input
                id="operario-email"
                type="email"
                className={`form-control ${errores.email ? 'is-invalid' : ''}`}
                value={form.email}
                onChange={setCampo('email')}
                maxLength={150}
                autoComplete="email"
                placeholder="operario@mandatodo.com"
              />
            </Field>
          </div>

          {!editando && (
            <div className="col-12">
              <Field
                label="Contraseña"
                htmlFor="operario-password"
                required
                error={errores.password}
                hint="Mínimo 8 caracteres."
              >
                <input
                  id="operario-password"
                  type="password"
                  className={`form-control ${errores.password ? 'is-invalid' : ''}`}
                  value={form.password}
                  onChange={setCampo('password')}
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                  placeholder="Mínimo 8 caracteres"
                />
              </Field>
            </div>
          )}

          <div className="col-12 col-md-6">
            <Field
              label="Rol"
              htmlFor="operario-rol"
              required
              error={errores.rol}
            >
              <select
                id="operario-rol"
                className={`form-select ${errores.rol ? 'is-invalid' : ''}`}
                value={form.rol}
                onChange={setCampo('rol')}
                disabled={saving}
              >
                {ROLES.map((rol) => (
                  <option key={rol.value} value={rol.value}>
                    {rol.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          {editando && (
            <div className="col-12 col-md-6">
              <Field
                label="Estado"
                htmlFor="operario-estado"
                required
                error={errores.estado}
                hint={esMiUsuario ? 'No podés cambiar tu propio estado.' : undefined}
              >
                <select
                  id="operario-estado"
                  className={`form-select ${errores.estado ? 'is-invalid' : ''}`}
                  value={form.estado}
                  onChange={setCampo('estado')}
                  disabled={saving || esMiUsuario}
                >
                  {ESTADOS.map((estado) => (
                    <option key={estado.value} value={estado.value}>
                      {estado.label}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
          )}
        </div>

        {!editando && (
          <p className="text-secondary small mb-0">
            El operario se crea en estado <strong>Activo</strong>.
          </p>
        )}
      </form>
    </Modal>
  )
}
