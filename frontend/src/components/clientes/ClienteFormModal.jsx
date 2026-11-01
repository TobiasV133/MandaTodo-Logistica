import { useState } from 'react'

import Field from '../ui/Field'
import Icon from '../ui/Icon'
import Modal from '../ui/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { actualizarCliente, crearCliente } from '../../services/clienteService'
import {
  TIPOS_CLIENTE,
  TIPOS_DOCUMENTO,
  soloDigitos,
  validarDocumento,
} from '../../utils/documentos'

function formInicial(cliente) {
  if (!cliente) {
    return {
      tipoDocumento: 'DNI',
      numeroDocumento: '',
      nombreRazonSocial: '',
      telefono: '',
      email: '',
      tipoCliente: 'Particular',
      esFrecuente: false,
    }
  }

  return {
    tipoDocumento: cliente.tipoDocumento ?? 'DNI',
    numeroDocumento: cliente.numeroDocumento ?? '',
    nombreRazonSocial: cliente.nombreRazonSocial ?? '',
    telefono: cliente.telefono ?? '',
    email: cliente.email ?? '',
    tipoCliente: cliente.tipoCliente ?? 'Particular',
    esFrecuente: Boolean(cliente.esFrecuente),
  }
}

export default function ClienteFormModal({ cliente, onClose, onSaved }) {
  const { operario } = useAuth()
  const editando = Boolean(cliente)
  const [form, setForm] = useState(() => formInicial(cliente))
  const [errores, setErrores] = useState({})
  const [serverError, setServerError] = useState('')
  const [saving, setSaving] = useState(false)

  const setCampo = (campo) => (event) => {
    const { type, checked, value } = event.target
    setForm((actual) => ({
      ...actual,
      [campo]: type === 'checkbox' ? checked : value,
    }))
    setErrores((actual) => ({ ...actual, [campo]: undefined }))
  }

  const cerrar = () => {
    if (!saving) onClose?.()
  }

  const validar = () => {
    const next = {}

    if (!editando) {
      if (!form.tipoDocumento) {
        next.tipoDocumento = 'Seleccioná el tipo de documento.'
      }

      const documentoError = validarDocumento(
        form.tipoDocumento,
        form.numeroDocumento,
      )
      if (documentoError) next.numeroDocumento = documentoError
    }

    if (!form.nombreRazonSocial.trim()) {
      next.nombreRazonSocial = 'El nombre o razón social es obligatorio.'
    } else if (form.nombreRazonSocial.trim().length > 150) {
      next.nombreRazonSocial = 'El nombre no puede superar los 150 caracteres.'
    }

    if (!form.telefono.trim()) {
      next.telefono = 'El teléfono es obligatorio.'
    } else if (form.telefono.trim().length > 40) {
      next.telefono = 'El teléfono no puede superar los 40 caracteres.'
    }

    if (!form.email.trim()) {
      next.email = 'El email es obligatorio.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      next.email = 'Ingresá un email válido.'
    } else if (form.email.trim().length > 150) {
      next.email = 'El email no puede superar los 150 caracteres.'
    }

    if (!form.tipoCliente) {
      next.tipoCliente = 'Seleccioná el tipo de cliente.'
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
        const data = await actualizarCliente(
          cliente.idCliente,
          {
            nombreRazonSocial: form.nombreRazonSocial.trim(),
            telefono: form.telefono.trim(),
            email: form.email.trim().toLowerCase(),
            tipoCliente: form.tipoCliente,
            esFrecuente: Boolean(form.esFrecuente),
          },
          operario?.idOperario,
        )
        onSaved?.(data, 'editar')
      } else {
        const data = await crearCliente(
          {
            tipoDocumento: form.tipoDocumento,
            numeroDocumento: soloDigitos(form.numeroDocumento),
            nombreRazonSocial: form.nombreRazonSocial.trim(),
            telefono: form.telefono.trim(),
            email: form.email.trim().toLowerCase(),
            tipoCliente: form.tipoCliente,
            esFrecuente: Boolean(form.esFrecuente),
          },
          operario?.idOperario,
        )
        onSaved?.(data, 'crear')
      }
    } catch (err) {
      setServerError(
        getApiErrorMessage(err, 'No se pudo guardar el cliente.'),
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={editando ? 'Editar cliente' : 'Nuevo cliente'}
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
            form="cliente-form"
            className="btn btn-primary"
            disabled={saving}
          >
            {saving ? (
              <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
            ) : (
              <Icon name="check" size={16} className="me-1" />
            )}
            {saving ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear cliente'}
          </button>
        </>
      }
    >
      <form id="cliente-form" onSubmit={guardar} noValidate>
        {serverError && (
          <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <Icon name="alert" size={18} />
            <span>{serverError}</span>
          </div>
        )}

        <div className="row g-3">
          <div className="col-12 col-md-4">
            <Field
              label="Tipo de documento"
              htmlFor="cliente-tipo-documento"
              required
              error={errores.tipoDocumento}
            >
              <select
                id="cliente-tipo-documento"
                className={`form-select ${errores.tipoDocumento ? 'is-invalid' : ''}`}
                value={form.tipoDocumento}
                onChange={setCampo('tipoDocumento')}
                disabled={editando || saving}
              >
                {TIPOS_DOCUMENTO.map((tipo) => (
                  <option key={tipo.value} value={tipo.value}>
                    {tipo.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="col-12 col-md-8">
            <Field
              label="Número de documento"
              htmlFor="cliente-numero-documento"
              required={!editando}
              error={errores.numeroDocumento}
            >
              <input
                id="cliente-numero-documento"
                className={`form-control ${errores.numeroDocumento ? 'is-invalid' : ''}`}
                value={form.numeroDocumento}
                onChange={setCampo('numeroDocumento')}
                disabled={editando || saving}
                inputMode="numeric"
                maxLength={20}
                autoComplete="off"
                placeholder="DNI o CUIT sin puntos"
              />
            </Field>
          </div>

          <div className="col-12">
            <Field
              label="Nombre o razón social"
              htmlFor="cliente-nombre"
              required
              error={errores.nombreRazonSocial}
            >
              <input
                id="cliente-nombre"
                className={`form-control ${errores.nombreRazonSocial ? 'is-invalid' : ''}`}
                value={form.nombreRazonSocial}
                onChange={setCampo('nombreRazonSocial')}
                maxLength={150}
                autoComplete="name"
                placeholder="Ej. Distribuidora del Sur S.A."
              />
            </Field>
          </div>

          <div className="col-12 col-md-6">
            <Field
              label="Teléfono"
              htmlFor="cliente-telefono"
              required
              error={errores.telefono}
            >
              <input
                id="cliente-telefono"
                className={`form-control ${errores.telefono ? 'is-invalid' : ''}`}
                value={form.telefono}
                onChange={setCampo('telefono')}
                maxLength={40}
                autoComplete="tel"
                placeholder="11 4567-8901"
              />
            </Field>
          </div>

          <div className="col-12 col-md-6">
            <Field
              label="Email"
              htmlFor="cliente-email"
              required
              error={errores.email}
            >
              <input
                id="cliente-email"
                type="email"
                className={`form-control ${errores.email ? 'is-invalid' : ''}`}
                value={form.email}
                onChange={setCampo('email')}
                maxLength={150}
                autoComplete="email"
                placeholder="contacto@empresa.com"
              />
            </Field>
          </div>

          <div className="col-12 col-md-6">
            <Field
              label="Tipo de cliente"
              htmlFor="cliente-tipo"
              required
              error={errores.tipoCliente}
            >
              <select
                id="cliente-tipo"
                className={`form-select ${errores.tipoCliente ? 'is-invalid' : ''}`}
                value={form.tipoCliente}
                onChange={setCampo('tipoCliente')}
                disabled={saving}
              >
                {TIPOS_CLIENTE.map((tipo) => (
                  <option key={tipo.value} value={tipo.value}>
                    {tipo.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="col-12 col-md-6 d-flex align-items-center">
            <div className="form-check mt-3">
              <input
                id="cliente-frecuente"
                className="form-check-input"
                type="checkbox"
                checked={form.esFrecuente}
                onChange={setCampo('esFrecuente')}
                disabled={saving}
              />
              <label className="form-check-label" htmlFor="cliente-frecuente">
                Cliente frecuente
              </label>
            </div>
          </div>
        </div>

        {editando && (
          <p className="text-secondary small mb-0 mt-2">
            El documento no se puede modificar desde esta pantalla.
          </p>
        )}
      </form>
    </Modal>
  )
}
