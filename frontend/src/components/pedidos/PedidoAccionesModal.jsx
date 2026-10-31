import { useRef, useState } from 'react'
import { createPortal } from 'react-dom'

import { getEstadoInfo } from '../../constants/estados'
import { getApiErrorMessage } from '../../services/api'
import {
  cambiarEstado,
  imprimirRemito,
  resolverRevision,
} from '../../services/pedidoService'
import { formatFecha, formatFechaHora, formatMoneda, formatPeso } from '../../utils/formatters'
import EstadoBadge from '../ui/EstadoBadge'
import Icon from '../ui/Icon'

const MOTIVOS_FALLA = ['Ausente', 'Dirección Inexistente', 'Dirección Incompleta']

function horaActual() {
  return new Date().toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function nombreEstado(estado) {
  return getEstadoInfo(estado).label
}

function Dato({ label, children }) {
  return (
    <div className="pedido-dato">
      <span>{label}</span>
      <strong>{children || '—'}</strong>
    </div>
  )
}

export default function PedidoAccionesModal({
  pedido,
  onClose,
  onUpdated,
}) {
  const descargaRef = useRef(null)
  const [pedidoActual, setPedidoActual] = useState(pedido)
  const [historial, setHistorial] = useState([])
  const [formulario, setFormulario] = useState(null)
  const [motivoFalla, setMotivoFalla] = useState(MOTIVOS_FALLA[0])
  const [notaFalla, setNotaFalla] = useState('')
  const [motivoCancelacion, setMotivoCancelacion] = useState('')
  const [motivoReimpresion, setMotivoReimpresion] = useState('')
  const [kilometros, setKilometros] = useState(pedido.kilometros ?? '')
  const [montoEnvio, setMontoEnvio] = useState(
    pedido.montoEnvio ?? pedido.precioTotal ?? '',
  )
  const [correccion, setCorreccion] = useState({
    calle: pedido.calle ?? '',
    altura: pedido.altura ?? '',
    pisoDepto: pedido.pisoDepto ?? '',
    localidad: pedido.localidad ?? '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [resultado, setResultado] = useState(null)

  const estado = pedidoActual.estado
  const esRevision = estado === 'EN_REVISION'
  const puedeRemito = !esRevision && estado !== 'CANCELADO_EN_TRANSITO'
  const monto = pedidoActual.montoEnvio ?? pedidoActual.precioTotal

  const ejecutar = async (accion) => {
    setLoading(true)
    setError('')
    setExito('')
    try {
      const data = await accion()
      setResultado(data)
      return data
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudo completar la operación.'))
      return null
    } finally {
      setLoading(false)
    }
  }

  const cambiar = async (nuevoEstado, extra = {}) => {
    const data = await ejecutar(() =>
      cambiarEstado(pedido.idPedido, { estado: nuevoEstado, ...extra }),
    )
    if (!data) return

    const actualizado = data.pedido
    setPedidoActual(actualizado)
    setResultado(nuevoEstado === 'FALLIDO' ? data : null)
    setHistorial((prev) => [...prev, { estado: actualizado.estado, hora: horaActual() }])

    if (nuevoEstado === 'FALLIDO') {
      setExito(
        `Entrega fallida registrada. Recargo aplicado: ${formatMoneda(
          data.recargoAplicado,
        )}. Nueva fecha programada: ${formatFecha(data.fechaReprogramada)}. Podés reintentar el envío desde este mismo panel.`,
      )
    } else {
      const esTerminal =
        actualizado.estado === 'ENTREGADO' || actualizado.estado === 'CANCELADO_EN_TRANSITO'
      setExito(
        esTerminal
          ? `El pedido pasó a ${nombreEstado(actualizado.estado)}. Es un estado terminal: no admite más cambios.`
          : `El pedido pasó a ${nombreEstado(actualizado.estado)}. Podés seguir cambiando el estado desde este panel.`,
      )
    }
    setFormulario(null)
    await onUpdated?.()
  }

  const resolver = async (accion) => {
    const payload = { accion }

    if (accion === 'APROBAR') {
      if (correccion.calle.trim()) payload.calle = correccion.calle.trim()
      if (correccion.altura !== '' && correccion.altura != null) {
        payload.altura = Number(correccion.altura)
      }
      if (correccion.pisoDepto.trim()) payload.pisoDepto = correccion.pisoDepto.trim()
      if (correccion.localidad.trim()) payload.localidad = correccion.localidad.trim()
      if (montoEnvio !== '' && montoEnvio != null) payload.montoEnvio = Number(montoEnvio)
    } else {
      payload.motivoFalla = motivoFalla
      if (notaFalla.trim()) payload.notaFalla = notaFalla.trim()
    }

    const data = await ejecutar(() => resolverRevision(pedido.idPedido, payload))
    if (!data) return

    setPedidoActual(data)
    setHistorial((prev) => [...prev, { estado: data.estado, hora: horaActual() }])
    setExito(
      accion === 'APROBAR'
        ? 'Revisión aprobada. El pedido pasó a PENDIENTE y ya podés gestionarlo desde este panel.'
        : `Revisión rechazada. Se registró la entrega fallida por ${motivoFalla}.`,
    )
    setFormulario(null)
    await onUpdated?.()
  }

  const descargarRemito = async () => {
    setLoading(true)
    setError('')
    setExito('')
    try {
      const data = await imprimirRemito(pedido.idPedido, {
        motivoReimpresion: motivoReimpresion.trim() || undefined,
        kilometros: kilometros === '' ? undefined : Number(kilometros),
      })
      const url = URL.createObjectURL(data.blob)
      if (descargaRef.current) {
        descargaRef.current.href = url
        descargaRef.current.download = `remito-${pedido.idPedido}.pdf`
        descargaRef.current.click()
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 15000)
      setExito(
        `Remito ${data.numero ?? ''} ${data.reimpresion ? 'duplicado' : 'original'} generado correctamente.`,
      )
      await onUpdated?.()
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'No se pudo generar el remito. Si ya se imprimió antes, ingresá el motivo de reimpresión.',
        ),
      )
    } finally {
      setLoading(false)
    }
  }

  return createPortal(
    <>
      <div
        className="modal fade show d-block pedido-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="pedido-modal-titulo"
      >
        <div className="modal-dialog modal-lg modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <div>
                <span className="page-eyebrow">Gestión del pedido</span>
                <h2 className="modal-title fs-5" id="pedido-modal-titulo">
                  {pedidoActual.codigoPedido}
                </h2>
              </div>
              <button
                type="button"
                className="btn-close"
                onClick={onClose}
                aria-label="Cerrar"
                disabled={loading}
              />
            </div>

            <div className="modal-body">
              <div className="pedido-modal__estado">
                <EstadoBadge estado={estado} />
                {pedidoActual.motivoRevision && (
                  <span className="pedido-modal__motivo">{pedidoActual.motivoRevision}</span>
                )}
              </div>

              {historial.length > 0 && (
                <div className="pedido-historial" role="status" aria-label="Cambios de estado de esta sesión">
                  <span className="pedido-historial__label">
                    <Icon name="history" size={15} />
                    Cambios en esta sesión
                  </span>
                  <div className="pedido-historial__cadena">
                    {historial.map((paso, indice) => (
                      <span className="pedido-historial__paso" key={`${paso.estado}-${indice}`}>
                        {indice > 0 && <Icon name="arrowRight" size={14} />}
                        <EstadoBadge estado={paso.estado} />
                        <small>{paso.hora}</small>
                      </span>
                    ))}
                  </div>
                </div>
              )}

              <div className="pedido-detalle">
                <Dato label="Cliente">{pedidoActual.nombreCliente}</Dato>
                <Dato label="Documento">{pedidoActual.numeroDocumento}</Dato>
                <Dato label="Dirección">{pedidoActual.direccionCompleta}</Dato>
                <Dato label="Paquete">
                  {pedidoActual.descripcionPaquete ?? pedidoActual.descripcion}
                </Dato>
                <Dato label="Peso">
                  {pedidoActual.pesoKg != null ? `${formatPeso(pedidoActual.pesoKg)} kg` : null}
                </Dato>
                <Dato label="Monto del envío">{formatMoneda(monto)}</Dato>
                <Dato label="Fecha de alta">{formatFechaHora(pedidoActual.fechaCreacion)}</Dato>
                <Dato label="Fecha programada">
                  {formatFecha(
                    pedidoActual.fechaProgramadaEntrega ?? pedidoActual.fechaProgramada,
                  )}
                </Dato>
              </div>

              {pedidoActual.motivoFalla && (
                <div className="alert alert-warning mt-3 mb-0" role="status">
                  <strong>Última entrega fallida:</strong> {pedidoActual.motivoFalla}
                </div>
              )}
              {pedidoActual.motivoCancelacion && (
                <div className="alert alert-secondary mt-3 mb-0" role="status">
                  <strong>Motivo de cancelación:</strong> {pedidoActual.motivoCancelacion}
                </div>
              )}

              {error && (
                <div className="alert alert-danger d-flex align-items-start gap-2 mt-3" role="alert">
                  <Icon name="alert" size={18} />
                  <span>{error}</span>
                </div>
              )}

              {exito && (
                <div className="alert alert-success d-flex align-items-start gap-2 mt-3" role="status">
                  <Icon name="checkCircle" size={18} />
                  <span>{exito}</span>
                </div>
              )}

              {resultado?.reprogramado && (
                <div className="pedido-reprogramacion">
                  <Icon name="calendar" size={17} />
                  <span>
                    Recargo de {formatMoneda(resultado.recargoAplicado)} · nueva fecha{' '}
                    {formatFecha(resultado.fechaReprogramada)}
                  </span>
                </div>
              )}

              {esRevision && (
                <section className="pedido-acciones__bloque">
                  <h3>
                    <Icon name="alert" size={18} />
                    Resolver revisión
                  </h3>
                  <p>
                    Corregí la dirección o el importe del envío y aprobá; también podés rechazar
                    indicando el motivo de la falla. Estas correcciones solo están disponibles en
                    pedidos en revisión.
                  </p>

                  {formulario === 'rechazar' ? (
                    <div className="pedido-formulario">
                      <label className="form-label" htmlFor="motivo-revision">
                        Motivo de la falla
                      </label>
                      <select
                        id="motivo-revision"
                        className="form-select"
                        value={motivoFalla}
                        onChange={(event) => setMotivoFalla(event.target.value)}
                      >
                        {MOTIVOS_FALLA.map((motivo) => (
                          <option key={motivo} value={motivo}>
                            {motivo}
                          </option>
                        ))}
                      </select>
                      <label className="form-label mt-3" htmlFor="nota-revision">
                        Nota (opcional)
                      </label>
                      <textarea
                        id="nota-revision"
                        className="form-control"
                        rows="2"
                        value={notaFalla}
                        onChange={(event) => setNotaFalla(event.target.value)}
                        placeholder="Detalle para el operario"
                      />
                      <div className="pedido-formulario__acciones">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setFormulario(null)}
                          disabled={loading}
                        >
                          Volver
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => resolver('RECHAZAR')}
                          disabled={loading}
                        >
                          Confirmar rechazo
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="pedido-formulario">
                      <p className="pedido-formulario__nota">
                        Podés corregir la dirección y el importe del envío. Los cambios se guardan
                        junto con la aprobación; si no necesitás cambios, aprobá directamente.
                      </p>
                      <div className="row g-2">
                        <div className="col-md-7">
                          <label className="form-label" htmlFor="correccion-calle">
                            Calle
                          </label>
                          <input
                            id="correccion-calle"
                            className="form-control"
                            value={correccion.calle}
                            onChange={(event) =>
                              setCorreccion((prev) => ({ ...prev, calle: event.target.value }))
                            }
                          />
                        </div>
                        <div className="col-md-5">
                          <label className="form-label" htmlFor="correccion-altura">
                            Altura
                          </label>
                          <input
                            id="correccion-altura"
                            type="number"
                            min="0"
                            className="form-control"
                            value={correccion.altura}
                            onChange={(event) =>
                              setCorreccion((prev) => ({ ...prev, altura: event.target.value }))
                            }
                          />
                        </div>
                        <div className="col-md-5">
                          <label className="form-label" htmlFor="correccion-piso">
                            Piso / depto
                          </label>
                          <input
                            id="correccion-piso"
                            className="form-control"
                            value={correccion.pisoDepto}
                            onChange={(event) =>
                              setCorreccion((prev) => ({ ...prev, pisoDepto: event.target.value }))
                            }
                          />
                        </div>
                        <div className="col-md-7">
                          <label className="form-label" htmlFor="correccion-localidad">
                            Localidad
                          </label>
                          <input
                            id="correccion-localidad"
                            className="form-control"
                            value={correccion.localidad}
                            onChange={(event) =>
                              setCorreccion((prev) => ({ ...prev, localidad: event.target.value }))
                            }
                          />
                        </div>
                        <div className="col-md-6">
                          <label className="form-label" htmlFor="correccion-monto">
                            Importe del envío
                          </label>
                          <input
                            id="correccion-monto"
                            type="number"
                            min="0"
                            step="0.01"
                            className="form-control"
                            value={montoEnvio}
                            onChange={(event) => setMontoEnvio(event.target.value)}
                          />
                        </div>
                      </div>
                      <div className="pedido-formulario__acciones">
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => setFormulario('rechazar')}
                          disabled={loading}
                        >
                          <Icon name="x" size={16} />
                          Rechazar
                        </button>
                        <button
                          type="button"
                          className="btn btn-success btn-sm"
                          onClick={() => resolver('APROBAR')}
                          disabled={loading}
                        >
                          <Icon name="checkCircle" size={16} />
                          Guardar y aprobar
                        </button>
                      </div>
                    </div>
                  )}
                </section>
              )}

              {!esRevision && (
                <section className="pedido-acciones__bloque">
                  <h3>
                    <Icon name="truck" size={18} />
                    Cambiar estado
                  </h3>

                  {formulario === 'fallido' ? (
                    <div className="pedido-formulario">
                      <label className="form-label" htmlFor="motivo-falla">
                        Motivo de la entrega fallida
                      </label>
                      <select
                        id="motivo-falla"
                        className="form-select"
                        value={motivoFalla}
                        onChange={(event) => setMotivoFalla(event.target.value)}
                      >
                        {MOTIVOS_FALLA.map((motivo) => (
                          <option key={motivo} value={motivo}>
                            {motivo}
                          </option>
                        ))}
                      </select>
                      <label className="form-label mt-3" htmlFor="nota-falla">
                        Nota (opcional)
                      </label>
                      <textarea
                        id="nota-falla"
                        className="form-control"
                        rows="2"
                        value={notaFalla}
                        onChange={(event) => setNotaFalla(event.target.value)}
                        placeholder="Detalle de lo ocurrido"
                      />
                      <p className="pedido-formulario__nota">
                        Se aplicará un recargo del 50 % y se reprogramará al siguiente día hábil.
                      </p>
                      <div className="pedido-formulario__acciones">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setFormulario(null)}
                          disabled={loading}
                        >
                          Volver
                        </button>
                        <button
                          type="button"
                          className="btn btn-warning btn-sm"
                          onClick={() =>
                            cambiar('FALLIDO', {
                              motivoFalla,
                              notaFalla: notaFalla.trim() || undefined,
                            })
                          }
                          disabled={loading}
                        >
                          Registrar falla
                        </button>
                      </div>
                    </div>
                  ) : formulario === 'cancelar' ? (
                    <div className="pedido-formulario">
                      <label className="form-label" htmlFor="motivo-cancelacion">
                        Motivo de la cancelación en tránsito
                      </label>
                      <textarea
                        id="motivo-cancelacion"
                        className="form-control"
                        rows="2"
                        value={motivoCancelacion}
                        onChange={(event) => setMotivoCancelacion(event.target.value)}
                        placeholder="Ej.: el cliente canceló el envío"
                      />
                      <div className="pedido-formulario__acciones">
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setFormulario(null)}
                          disabled={loading}
                        >
                          Volver
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() =>
                            cambiar('CANCELADO_EN_TRANSITO', {
                              motivoCancelacion: motivoCancelacion.trim(),
                            })
                          }
                          disabled={loading || !motivoCancelacion.trim()}
                        >
                          Confirmar cancelación
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="pedido-acciones__botones">
                      {estado === 'PENDIENTE' && (
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => cambiar('EN_TRANSITO')}
                          disabled={loading}
                        >
                          <Icon name="truck" size={16} />
                          Pasar a en tránsito
                        </button>
                      )}

                      {estado === 'EN_TRANSITO' && (
                        <>
                          <button
                            type="button"
                            className="btn btn-success btn-sm"
                            onClick={() => cambiar('ENTREGADO')}
                            disabled={loading}
                          >
                            <Icon name="checkCircle" size={16} />
                            Marcar entregado
                          </button>
                          <button
                            type="button"
                            className="btn btn-warning btn-sm"
                            onClick={() => setFormulario('fallido')}
                            disabled={loading}
                          >
                            <Icon name="alert" size={16} />
                            Registrar entrega fallida
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm"
                            onClick={() => setFormulario('cancelar')}
                            disabled={loading}
                          >
                            <Icon name="x" size={16} />
                            Cancelar en tránsito
                          </button>
                        </>
                      )}

                      {estado === 'FALLIDO' && (
                        <>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => cambiar('EN_TRANSITO')}
                            disabled={loading}
                          >
                            <Icon name="refresh" size={16} />
                            Reintentar envío
                          </button>
                          <button
                            type="button"
                            className="btn btn-success btn-sm"
                            onClick={() => cambiar('ENTREGADO')}
                            disabled={loading}
                          >
                            <Icon name="checkCircle" size={16} />
                            Marcar entregado
                          </button>
                        </>
                      )}

                      {(estado === 'ENTREGADO' || estado === 'CANCELADO_EN_TRANSITO') && (
                        <span className="text-muted">
                          El pedido está en un estado terminal y no admite cambios.
                        </span>
                      )}
                    </div>
                  )}
                </section>
              )}

              {puedeRemito && (
                <section className="pedido-acciones__bloque">
                  <h3>
                    <Icon name="package" size={18} />
                    Remito
                  </h3>
                  <div className="row g-2 align-items-end">
                    <div className="col-md-7">
                      <label className="form-label" htmlFor="motivo-reimpresion">
                        Motivo de reimpresión (obligatorio solo si ya se imprimió)
                      </label>
                      <input
                        id="motivo-reimpresion"
                        className="form-control"
                        value={motivoReimpresion}
                        onChange={(event) => setMotivoReimpresion(event.target.value)}
                        placeholder="Ej.: se perdió la copia firmada"
                      />
                    </div>
                    <div className="col-md-3">
                      <label className="form-label" htmlFor="remito-km">
                        Kilómetros del recorrido
                      </label>
                      <input
                        id="remito-km"
                        type="number"
                        min="0"
                        step="0.1"
                        className="form-control"
                        value={kilometros}
                        onChange={(event) => setKilometros(event.target.value)}
                      />
                    </div>
                    <div className="col-md-2">
                      <button
                        type="button"
                        className="btn btn-outline-primary w-100"
                        onClick={descargarRemito}
                        disabled={loading}
                      >
                        <Icon name="package" size={16} />
                        PDF
                      </button>
                    </div>
                  </div>
                  <p className="pedido-formulario__nota mt-2">
                    La primera impresión sale como <strong>Remito Original</strong> y no pide
                    motivo. Si ya se imprimió antes, el PDF sale como{' '}
                    <strong>Remito Duplicado</strong> y el motivo es obligatorio. Se imprimen los
                    kilómetros cargados al dar de alta el pedido; si los corregís acá, esa
                    corrección es la que sale en el PDF.
                  </p>
                </section>
              )}
            </div>

            <div className="modal-footer">
              <a ref={descargaRef} className="d-none" aria-hidden="true">
                Descargar remito
              </a>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={onClose}
                disabled={loading}
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show" />
    </>,
    document.body,
  )
}
