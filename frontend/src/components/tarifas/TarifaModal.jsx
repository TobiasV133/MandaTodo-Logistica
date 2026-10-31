import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'

import { useTarifa } from '../../context/TarifaContext'
import { getApiErrorMessage } from '../../services/api'
import { normalizarTarifa, TARIFA_POR_DEFECTO } from '../../utils/tarifas'
import Icon from '../ui/Icon'

const CAMPOS = [
  { name: 'tarifaBase', label: 'Tarifa base', unit: '$', step: '0.01', grupo: 'importes' },
  { name: 'tarifaMedia', label: 'Tarifa media', unit: '$', step: '0.01', grupo: 'importes' },
  { name: 'recargoPorKm', label: 'Recargo por km extra', unit: '$', step: '0.01', grupo: 'importes' },
  { name: 'recargoPorKg', label: 'Recargo por kg extra', unit: '$', step: '0.01', grupo: 'importes' },
  { name: 'limitePesoBase', label: 'Nivel 1 · peso máximo', unit: 'kg', step: '0.001', grupo: 'limites' },
  { name: 'limiteKmBase', label: 'Nivel 1 · km máximos', unit: 'km', step: '0.001', grupo: 'limites' },
  { name: 'limitePesoMedio', label: 'Nivel 2 · peso máximo', unit: 'kg', step: '0.001', grupo: 'limites' },
  { name: 'limiteKmMedio', label: 'Nivel 2 · km máximos', unit: 'km', step: '0.001', grupo: 'limites' },
  { name: 'maxPesoKg', label: 'Capacidad máxima de peso', unit: 'kg', step: '0.001', grupo: 'limites' },
  { name: 'maxKilometros', label: 'Capacidad máxima de distancia', unit: 'km', step: '0.001', grupo: 'limites' },
]

const CAMPOS_IMPORTES = CAMPOS.filter((campo) => campo.grupo === 'importes')
const CAMPOS_LIMITES = CAMPOS.filter((campo) => campo.grupo === 'limites')

function aFormulario(valores) {
  const normalizada = normalizarTarifa(valores)
  return Object.fromEntries(CAMPOS.map(({ name }) => [name, String(normalizada[name])]))
}

function validar(valores) {
  if (!Object.values(valores).every((valor) => Number.isFinite(valor) && valor >= 0)) {
    return 'Completá todos los valores con números mayores o iguales a 0.'
  }
  if (CAMPOS_LIMITES.some(({ name }) => valores[name] <= 0)) {
    return 'Los límites de peso, kilómetros y la capacidad máxima deben ser mayores a 0.'
  }
  if (valores.tarifaMedia < valores.tarifaBase) {
    return 'La tarifa media no puede ser menor que la tarifa base.'
  }
  if (valores.limitePesoBase >= valores.limitePesoMedio) {
    return 'El peso máximo del nivel 1 debe ser menor que el del nivel 2.'
  }
  if (valores.limiteKmBase >= valores.limiteKmMedio) {
    return 'Los kilómetros máximos del nivel 1 deben ser menores que los del nivel 2.'
  }
  if (valores.limitePesoMedio >= valores.maxPesoKg) {
    return 'La capacidad máxima de peso debe ser mayor que el límite del nivel 2.'
  }
  if (valores.limiteKmMedio >= valores.maxKilometros) {
    return 'La capacidad máxima de distancia debe ser mayor que el límite del nivel 2.'
  }
  return ''
}

export default function TarifaModal({ onClose }) {
  const { tarifa, actualizar } = useTarifa()
  const [formulario, setFormulario] = useState(() => aFormulario(tarifa))
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape' && !guardando) onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [guardando, onClose])

  const cambiar = (campo, valor) => {
    setFormulario((prev) => ({ ...prev, [campo]: valor }))
  }

  const restaurar = () => {
    setFormulario(aFormulario(TARIFA_POR_DEFECTO))
    setError('')
    setExito('')
  }

  const guardar = async (event) => {
    event.preventDefault()
    if (CAMPOS.some(({ name }) => String(formulario[name]).trim() === '')) {
      setExito('')
      setError('Completá todos los valores del tarifario.')
      return
    }
    const valores = Object.fromEntries(
      CAMPOS.map(({ name }) => [name, Number(formulario[name])]),
    )
    const mensajeValidacion = validar(valores)
    if (mensajeValidacion) {
      setExito('')
      setError(mensajeValidacion)
      return
    }

    setGuardando(true)
    setError('')
    setExito('')
    try {
      await actualizar(valores)
      setExito('Tarifario actualizado. Los próximos pedidos usan estos valores.')
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudo guardar el tarifario.'))
    } finally {
      setGuardando(false)
    }
  }

  return createPortal(
    <>
      <div
        className="modal-backdrop fade show"
        aria-hidden="true"
        onClick={guardando ? undefined : onClose}
      />
      <div
        className="modal fade show d-block pedido-modal tarifa-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tarifa-modal-titulo"
        onClick={(event) => {
          if (event.target === event.currentTarget && !guardando) onClose()
        }}
      >
        <div className="modal-dialog modal-lg modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <div>
                <span className="page-eyebrow">Configuración · Solo administradores</span>
                <h2 className="modal-title fs-5" id="tarifa-modal-titulo">
                  Tarifario de envíos
                </h2>
              </div>
              <button
                type="button"
                className="btn-close"
                onClick={onClose}
                aria-label="Cerrar"
                disabled={guardando}
              />
            </div>

            <form onSubmit={guardar}>
              <div className="modal-body">
                <p className="text-muted mb-3">
                  Editá los importes y los límites de los tres niveles. Los cambios se aplican a
                  los pedidos nuevos y quedan registrados en la auditoría.
                </p>

                <section className="pedido-acciones__bloque">
                  <h3>
                    <Icon name="money" size={18} />
                    Importes
                  </h3>
                  <div className="row g-2">
                    {CAMPOS_IMPORTES.map((campo) => (
                      <div className="col-md-6" key={campo.name}>
                        <label className="form-label" htmlFor={`tarifa-${campo.name}`}>
                          {campo.label}
                        </label>
                        <div className="input-group">
                          <span className="input-group-text">{campo.unit}</span>
                          <input
                            id={`tarifa-${campo.name}`}
                            type="number"
                            className="form-control"
                            min="0"
                            step={campo.step}
                            value={formulario[campo.name]}
                            onChange={(event) => cambiar(campo.name, event.target.value)}
                            disabled={guardando}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                <section className="pedido-acciones__bloque">
                  <h3>
                    <Icon name="truck" size={18} />
                    Niveles y capacidad
                  </h3>
                  <div className="row g-2">
                    {CAMPOS_LIMITES.map((campo) => (
                      <div className="col-md-6" key={campo.name}>
                        <label className="form-label" htmlFor={`tarifa-${campo.name}`}>
                          {campo.label}
                        </label>
                        <div className="input-group">
                          <input
                            id={`tarifa-${campo.name}`}
                            type="number"
                            className="form-control"
                            min="0"
                            step={campo.step}
                            value={formulario[campo.name]}
                            onChange={(event) => cambiar(campo.name, event.target.value)}
                            disabled={guardando}
                          />
                          <span className="input-group-text">{campo.unit}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>

                {error && (
                  <div className="alert alert-danger d-flex align-items-start gap-2 mt-3 mb-0" role="alert">
                    <Icon name="alert" size={18} />
                    <span>{error}</span>
                  </div>
                )}

                {exito && (
                  <div className="alert alert-success d-flex align-items-start gap-2 mt-3 mb-0" role="status">
                    <Icon name="checkCircle" size={18} />
                    <span>{exito}</span>
                  </div>
                )}
              </div>

              <div className="modal-footer justify-content-between">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={restaurar}
                  disabled={guardando}
                >
                  Restaurar por defecto
                </button>
                <div className="d-flex gap-2">
                  <button
                    type="button"
                    className="btn btn-light btn-sm"
                    onClick={onClose}
                    disabled={guardando}
                  >
                    Cerrar
                  </button>
                  <button type="submit" className="btn btn-primary btn-sm" disabled={guardando}>
                    {guardando ? 'Guardando…' : 'Guardar tarifario'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
