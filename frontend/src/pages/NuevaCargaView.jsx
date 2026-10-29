import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import AddressMap from '../components/map/AddressMap'
import EstadoBadge from '../components/ui/EstadoBadge'
import Field from '../components/ui/Field'
import Icon from '../components/ui/Icon'
import ResumenPedido from '../components/wizard/ResumenPedido'
import Stepper from '../components/wizard/Stepper'
import { useResumen } from '../context/ResumenContext'
import { getApiErrorMessage } from '../services/api'
import { buscarPorDocumento } from '../services/clienteService'
import { normalizarDireccion } from '../services/geoService'
import { crearPedido } from '../services/pedidoService'
import {
  documentoCompleto,
  formatearDocumento,
  longitudDocumento,
  soloDigitos,
  TIPOS_CLIENTE,
  TIPOS_DOCUMENTO,
  validarDocumento,
} from '../utils/documentos'
import {
  formatFechaLarga,
  formatMoneda,
} from '../utils/formatters'
import {
  calcularTarifa,
  MAX_KILOMETROS,
  MAX_PESO_KG,
  MENSAJE_CAPACIDAD,
  RECARGO_POR_KG,
  RECARGO_POR_KM,
  TARIFA_BASE,
  TARIFA_MEDIA,
} from '../utils/tarifas'

const CLIENTE_INICIAL = {
  tipoDocumento: 'DNI',
  numeroDocumento: '',
  nombreRazonSocial: '',
  telefono: '',
  email: '',
  tipoCliente: 'Particular',
  esFrecuente: false,
}

const DIRECCION_INICIAL = {
  calle: '',
  altura: '',
  pisoDepto: '',
  localidad: '',
}

const DETALLE_INICIAL = {
  descripcionPaquete: '',
  pesoKg: '',
  kilometros: '',
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function StepHeader({ number, title, description }) {
  return (
    <div className="step-heading">
      <span className="step-heading__number">{number}</span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </div>
  )
}

export default function NuevaCargaView() {
  const navigate = useNavigate()
  const { refresh: refreshResumen } = useResumen()

  const [paso, setPaso] = useState(1)
  const [cliente, setCliente] = useState(CLIENTE_INICIAL)
  const [clienteExistente, setClienteExistente] = useState(null)
  const [buscaCliente, setBuscaCliente] = useState(false)
  const [consultaRealizada, setConsultaRealizada] = useState(false)
  const [errorCliente, setErrorCliente] = useState('')

  const [direccion, setDireccion] = useState(DIRECCION_INICIAL)
  const [geo, setGeo] = useState(null)
  const [claveValidada, setClaveValidada] = useState('')
  const [pinAjustado, setPinAjustado] = useState(null)
  const [validandoGeo, setValidandoGeo] = useState(false)
  const [errorGeo, setErrorGeo] = useState('')

  const [detalle, setDetalle] = useState(DETALLE_INICIAL)
  const [errores, setErrores] = useState({})

  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState('')
  const [pedidoCreado, setPedidoCreado] = useState(null)

  const actualizarUbicacion = useCallback(({ latitud: nuevaLatitud, longitud: nuevaLongitud }) => {
    setGeo((prev) =>
      prev ? { ...prev, latitud: nuevaLatitud, longitud: nuevaLongitud } : prev,
    )
  }, [])

  const claveDireccion = `${direccion.calle.trim().toLowerCase()}|${direccion.altura.trim()}|${direccion.localidad
    .trim()
    .toLowerCase()}`
  const direccionValidada = geo !== null && claveValidada === claveDireccion

  const alturaTexto = direccion.altura.trim().toLowerCase()
  const alturaVacia = !alturaTexto
  const alturaSinNumero = !alturaVacia && (alturaTexto === '0' || ['s/n', 'sn', 'sin numero'].includes(alturaTexto))
  const alturaInvalida = alturaVacia || alturaSinNumero
  const calleSinNumero = direccion.calle.trim().toLowerCase() === 's/n'
  const esAmbigua = Boolean(geo?.esAmbigua) || alturaInvalida || calleSinNumero

  const geoEfectivo = useMemo(() => {
    if (!geo) return null
    if (!pinAjustado) return geo
    return { ...geo, latitud: pinAjustado.latitud, longitud: pinAjustado.longitud }
  }, [geo, pinAjustado])

  const ajustarPin = (latitud, longitud) => {
    if (!Number.isFinite(latitud) || !Number.isFinite(longitud)) return
    setPinAjustado({ latitud, longitud })
  }

  const tarifa = useMemo(() => {
    try {
      return calcularTarifa(detalle.pesoKg, detalle.kilometros)
    } catch (error) {
      return {
        precioBase: 0,
        recargo: 0,
        precioTotal: 0,
        error: error instanceof Error ? error.message : MENSAJE_CAPACIDAD,
      }
    }
  }, [detalle.pesoKg, detalle.kilometros])

  const prediccion = useMemo(() => {
    if (esAmbigua || !direccionValidada) {
      return {
        tone: 'warning',
        icon: 'alert',
        title: 'Ingresará a EN REVISIÓN',
        detail: 'La dirección es ambigua o está incompleta y requiere validación manual.',
      }
    }
    if (cliente.tipoCliente === 'Empresa') {
      return {
        tone: 'info',
        icon: 'info',
        title: 'Posible revisión comercial',
        detail: 'Si ya existe un pedido idéntico hoy, el backend lo enviará a revisión.',
      }
    }
    return {
      tone: 'success',
      icon: 'checkCircle',
      title: 'Ingresará como PENDIENTE',
      detail: 'El pedido cumple las condiciones para pasar directo a la bandeja de despacho.',
    }
  }, [cliente.tipoCliente, esAmbigua, direccionValidada])

  useEffect(() => {
    const completo = documentoCompleto(cliente.tipoDocumento, cliente.numeroDocumento)

    if (!completo) {
      return undefined
    }

    let activo = true
    const timeout = window.setTimeout(async () => {
      setBuscaCliente(true)
      setErrorCliente('')

      try {
        const encontrado = await buscarPorDocumento(cliente.numeroDocumento)
        if (!activo) return

        setClienteExistente(encontrado)
        setConsultaRealizada(true)
        setCliente((prev) => ({
          ...prev,
          nombreRazonSocial: encontrado.nombreRazonSocial ?? '',
          telefono: encontrado.telefono ?? '',
          email: encontrado.email ?? '',
          tipoCliente: encontrado.tipoCliente ?? 'Particular',
          esFrecuente: Boolean(encontrado.esFrecuente),
        }))
      } catch (err) {
        if (!activo) return
        setClienteExistente(null)
        setConsultaRealizada(true)

        if (err?.response?.status !== 404) {
          setErrorCliente(getApiErrorMessage(err, 'No se pudo buscar el cliente.'))
        }
      } finally {
        if (activo) setBuscaCliente(false)
      }
    }, 650)

    return () => {
      activo = false
      window.clearTimeout(timeout)
    }
  }, [cliente.tipoDocumento, cliente.numeroDocumento])

  const cambiarTipoDocumento = (event) => {
    const tipoDocumento = event.target.value
    setCliente({ ...CLIENTE_INICIAL, tipoDocumento })
    setClienteExistente(null)
    setBuscaCliente(false)
    setConsultaRealizada(false)
    setErrorCliente('')
    setErrores({})
  }

  const cambiarDocumento = (event) => {
    const numeroDocumento = soloDigitos(event.target.value).slice(
      0,
      longitudDocumento(cliente.tipoDocumento),
    )

    setCliente((prev) => ({
      ...prev,
      numeroDocumento,
      ...(clienteExistente
        ? {
            nombreRazonSocial: '',
            telefono: '',
            email: '',
            tipoCliente: 'Particular',
            esFrecuente: false,
          }
        : {}),
    }))
    setClienteExistente(null)
    setBuscaCliente(false)
    setConsultaRealizada(false)
    setErrorCliente('')
    setErrores((prev) => ({ ...prev, numeroDocumento: undefined }))
  }

  const cambiarCliente = (event) => {
    const { name, value, type, checked } = event.target
    setCliente((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
    setErrores((prev) => ({ ...prev, [name]: undefined }))
  }

  const cambiarDireccion = (event) => {
    const { name, value } = event.target
    setDireccion((prev) => ({ ...prev, [name]: value }))
    setErrorGeo('')
  }

  const cambiarDetalle = (event) => {
    const { name, value } = event.target
    setDetalle((prev) => ({ ...prev, [name]: value }))
    setErrores((prev) => ({ ...prev, [name]: undefined }))
  }

  const validarPaso1 = () => {
    const nuevos = {}
    const errorDocumento = validarDocumento(cliente.tipoDocumento, cliente.numeroDocumento)
    if (errorDocumento) nuevos.numeroDocumento = errorDocumento
    if (!cliente.nombreRazonSocial.trim()) {
      nuevos.nombreRazonSocial = 'Ingresá el nombre o la razón social.'
    }
    if (!cliente.email.trim()) {
      nuevos.email = 'Ingresá el email del cliente.'
    } else if (!EMAIL_REGEX.test(cliente.email.trim())) {
      nuevos.email = 'El email no tiene un formato válido.'
    }

    setErrores(nuevos)
    return Object.keys(nuevos).length === 0
  }

  const validarPaso2 = () => {
    if (!direccion.calle.trim()) {
      setErrorGeo('Ingresá la calle antes de validar la dirección.')
      return false
    }
    if (!direccion.localidad.trim()) {
      setErrorGeo('Ingresá la localidad antes de validar la dirección.')
      return false
    }
    if (!direccionValidada) {
      setErrorGeo('Validá la dirección con el botón “Validar dirección” para continuar.')
      return false
    }
    return true
  }

  const validarPaso3 = () => {
    const nuevos = {}
    if (!detalle.descripcionPaquete.trim()) {
      nuevos.descripcionPaquete = 'Describí el paquete que se va a enviar.'
    }
    const peso = Number(detalle.pesoKg)
    if (!Number.isFinite(peso) || peso <= 0) {
      nuevos.pesoKg = 'El peso debe ser mayor a 0.'
    } else if (peso > MAX_PESO_KG) {
      nuevos.pesoKg = MENSAJE_CAPACIDAD
    }
    const kilometros = Number(detalle.kilometros)
    if (!Number.isFinite(kilometros) || kilometros < 0 || detalle.kilometros === '') {
      nuevos.kilometros = 'Ingresá los kilómetros del recorrido.'
    } else if (kilometros > MAX_KILOMETROS) {
      nuevos.kilometros = MENSAJE_CAPACIDAD
    }
    setErrores(nuevos)
    return Object.keys(nuevos).length === 0
  }

  const avanzar = () => {
    if (paso === 1 && !validarPaso1()) return
    if (paso === 2 && !validarPaso2()) return
    if (paso === 3 && !validarPaso3()) return

    setErrorEnvio('')
    setPaso((prev) => Math.min(prev + 1, 4))
  }

  const retroceder = () => {
    setErrorEnvio('')
    setPaso((prev) => Math.max(prev - 1, 1))
  }

  const validarDireccion = async () => {
    const calle = direccion.calle.trim()
    const localidad = direccion.localidad.trim()

    if (!calle || !localidad) {
      setErrorGeo('Completá calle y localidad para validar la dirección.')
      return
    }

    setValidandoGeo(true)
    setErrorGeo('')
    setPinAjustado(null)

    try {
      const data = await normalizarDireccion({
        calle,
        altura: direccion.altura.trim(),
        localidad,
      })

      if (data?.latitud == null || data?.longitud == null) {
        throw new Error('La dirección no devolvió coordenadas válidas.')
      }

      setGeo(data)
      setClaveValidada(claveDireccion)
    } catch (err) {
      setGeo(null)
      setClaveValidada('')
      setErrorGeo(
        err?.isAxiosError
          ? getApiErrorMessage(err, 'No se pudo validar la dirección.')
          : 'No se pudo validar la dirección. Revisá los datos e intentá nuevamente.',
      )
    } finally {
      setValidandoGeo(false)
    }
  }

  const confirmarPedido = async () => {
    if (!direccionValidada || !geoEfectivo) {
      setErrorEnvio('La dirección debe estar validada antes de confirmar el pedido.')
      return
    }

    const alturaDigitos = soloDigitos(direccion.altura)
    const altura = alturaInvalida || !alturaDigitos ? null : Number(alturaDigitos)

    const payload = {
      cliente: {
        tipoDocumento: cliente.tipoDocumento,
        numeroDocumento: soloDigitos(cliente.numeroDocumento),
        nombreRazonSocial: cliente.nombreRazonSocial.trim(),
        telefono: cliente.telefono.trim() || null,
        email: cliente.email.trim(),
        tipoCliente: cliente.tipoCliente,
        esFrecuente: Boolean(cliente.esFrecuente),
      },
      descripcionPaquete: detalle.descripcionPaquete.trim(),
      pesoKg: Number(detalle.pesoKg),
      kilometros: Number(detalle.kilometros),
      calle: direccion.calle.trim(),
      altura,
      pisoDepto: direccion.pisoDepto.trim() || null,
      localidad: direccion.localidad.trim(),
      latitud: Number(geoEfectivo.latitud),
      longitud: Number(geoEfectivo.longitud),
      esAmbigua: Boolean(esAmbigua),
      pinAjustado: Boolean(pinAjustado),
      observacionGeo: geo.observacion ?? null,
    }

    setEnviando(true)
    setErrorEnvio('')

    try {
      const creado = await crearPedido(payload)
      setPedidoCreado(creado)
      await refreshResumen()
    } catch (err) {
      setErrorEnvio(getApiErrorMessage(err, 'No se pudo registrar el pedido. Intentá nuevamente.'))
    } finally {
      setEnviando(false)
    }
  }

  const reiniciar = () => {
    setPaso(1)
    setCliente({ ...CLIENTE_INICIAL })
    setClienteExistente(null)
    setBuscaCliente(false)
    setConsultaRealizada(false)
    setErrorCliente('')
    setDireccion({ ...DIRECCION_INICIAL })
    setGeo(null)
    setClaveValidada('')
    setPinAjustado(null)
    setErrorGeo('')
    setDetalle({ ...DETALLE_INICIAL })
    setErrores({})
    setErrorEnvio('')
    setPedidoCreado(null)
  }

  if (pedidoCreado) {
    const creado = pedidoCreado.pedido ?? pedidoCreado
    const motivosRetencion = pedidoCreado.retencion?.motivos?.join(' y ')

    return (
      <div className="wizard-card">
        <div className="success-card">
          <span className="success-card__icon">
            <Icon name="checkCircle" size={34} strokeWidth={2} />
          </span>
          <span className="page-eyebrow">Alta completada</span>
          <h2>Pedido registrado</h2>
          <p>
            El pedido <strong>{creado.codigoPedido}</strong> se guardó correctamente el{' '}
            {formatFechaLarga()}.
          </p>

          <div className="success-card__badge">
            <EstadoBadge estado={creado.estado} />
          </div>

          {creado.estado === 'EN_REVISION' && (
            <div className="alert alert-warning text-start" role="status">
              <strong>El pedido quedó retenido en revisión.</strong>
              <div>
                {motivosRetencion ||
                  creado.motivoRevision ||
                  'Requiere validación manual antes de despacharse.'}
              </div>
            </div>
          )}

          <div className="success-summary">
            <div>
              <span>Cliente</span>
              <strong>{creado.nombreCliente}</strong>
            </div>
            <div>
              <span>Dirección</span>
              <strong>{creado.direccionCompleta}</strong>
            </div>
            <div>
              <span>Total</span>
              <strong>{formatMoneda(creado.montoEnvio ?? creado.precioTotal)}</strong>
            </div>
          </div>

          <div className="success-card__actions">
            <button type="button" className="btn btn-outline-secondary" onClick={reiniciar}>
              <Icon name="plus" size={17} />
              Registrar otro pedido
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navigate(creado.estado === 'EN_REVISION' ? '/en-revision' : '/pedidos')}
            >
              {creado.estado === 'EN_REVISION' ? 'Ir a la bandeja' : 'Ver pedidos'}
              <Icon name="arrowRight" size={17} />
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="wizard-card">
      <div className="wizard-head">
        <div>
          <span className="page-eyebrow">Nueva carga</span>
          <h2>Registrar pedido</h2>
          <p>Completá los 4 pasos para dar de alta la carga.</p>
        </div>
        <span className="wizard-step-count">Paso {paso} de 4</span>
      </div>

      <div className="wizard-stepper-wrap">
        <Stepper paso={paso} />
      </div>

      <div className="wizard-body">
        {paso === 1 && (
          <section>
            <StepHeader
              number={1}
              title="Identificación del cliente"
              description="Ingresá el documento para autocompletar los datos si el cliente ya existe."
            />

            <div className="row g-3">
              <div className="col-md-4">
                <Field label="Tipo de documento" htmlFor="tipo-documento" required>
                  <select
                    id="tipo-documento"
                    className="form-select"
                    value={cliente.tipoDocumento}
                    onChange={cambiarTipoDocumento}
                  >
                    {TIPOS_DOCUMENTO.map((tipo) => (
                      <option key={tipo.value} value={tipo.value}>
                        {tipo.label}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>

              <div className="col-md-8">
                <Field
                  label="Número"
                  htmlFor="numero-documento"
                  required
                  error={errores.numeroDocumento}
                  hint={`Sin puntos ni guiones. ${
                    cliente.tipoDocumento === 'CUIT' ? '11 dígitos' : '7 u 8 dígitos'
                  }.`}
                >
                  <div className="input-with-icon input-with-status">
                    <Icon name="search" size={18} />
                    <input
                      id="numero-documento"
                      type="text"
                      inputMode="numeric"
                      className={`form-control ${errores.numeroDocumento ? 'is-invalid' : ''}`}
                      placeholder={cliente.tipoDocumento === 'CUIT' ? '20123456789' : '12345678'}
                      value={cliente.numeroDocumento}
                      onChange={cambiarDocumento}
                      maxLength={longitudDocumento(cliente.tipoDocumento)}
                      autoComplete="off"
                    />
                    {buscaCliente && (
                      <span className="input-status-spinner">
                        <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                      </span>
                    )}
                  </div>
                </Field>
              </div>
            </div>

            {clienteExistente && (
              <div className="lookup-note lookup-note--success">
                <Icon name="checkCircle" size={18} />
                <div>
                  <strong>Cliente registrado</strong>
                  <span>
                    {clienteExistente.nombreRazonSocial} ·{' '}
                    {formatearDocumento(clienteExistente.tipoDocumento, clienteExistente.numeroDocumento)}
                    {clienteExistente.esFrecuente ? ' · Cliente frecuente' : ''}
                  </span>
                </div>
              </div>
            )}

            {!clienteExistente && consultaRealizada && !buscaCliente && !errorCliente && (
              <div className="lookup-note">
                <Icon name="info" size={18} />
                <div>
                  <strong>Cliente nuevo</strong>
                  <span>No hay coincidencias para ese documento. Completá los datos para crearlo.</span>
                </div>
              </div>
            )}

            {errorCliente && (
              <div className="alert alert-warning d-flex align-items-center gap-2" role="alert">
                <Icon name="alert" size={18} />
                {errorCliente}
              </div>
            )}

            <div className="row g-3 mt-1">
              <div className="col-md-7">
                <Field
                  label="Nombre / Razón social"
                  htmlFor="nombre-razon-social"
                  required
                  error={errores.nombreRazonSocial}
                >
                  <input
                    id="nombre-razon-social"
                    type="text"
                    name="nombreRazonSocial"
                    className={`form-control ${errores.nombreRazonSocial ? 'is-invalid' : ''}`}
                    value={cliente.nombreRazonSocial}
                    onChange={cambiarCliente}
                    placeholder="Ej.: Juan Pérez o Logística Sur SRL"
                  />
                </Field>
              </div>
              <div className="col-md-5">
                <Field label="Tipo de cliente" htmlFor="tipo-cliente" required>
                  <select
                    id="tipo-cliente"
                    name="tipoCliente"
                    className="form-select"
                    value={cliente.tipoCliente}
                    onChange={cambiarCliente}
                  >
                    {TIPOS_CLIENTE.map((tipo) => (
                      <option key={tipo.value} value={tipo.value}>
                        {tipo.label}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Teléfono" htmlFor="telefono">
                  <input
                    id="telefono"
                    type="tel"
                    name="telefono"
                    className="form-control"
                    value={cliente.telefono}
                    onChange={cambiarCliente}
                    placeholder="11 5555-5555"
                  />
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Email" htmlFor="email" required error={errores.email}>
                  <input
                    id="email"
                    type="email"
                    name="email"
                    className={`form-control ${errores.email ? 'is-invalid' : ''}`}
                    value={cliente.email}
                    onChange={cambiarCliente}
                    placeholder="cliente@correo.com"
                    autoComplete="email"
                  />
                </Field>
              </div>
            </div>

            <div className={`frequent-toggle ${cliente.esFrecuente ? 'is-active' : ''}`}>
              <div className="frequent-toggle__copy">
                <strong>
                  <Icon name="star" size={16} />
                  Cliente frecuente
                </strong>
                <p>Se usa para identificar clientes recurrentes; no exime las reglas de revisión por dirección.</p>
              </div>
              <div className="form-check form-switch">
                <input
                  className="form-check-input"
                  type="checkbox"
                  role="switch"
                  id="es-frecuente"
                  name="esFrecuente"
                  checked={cliente.esFrecuente}
                  onChange={cambiarCliente}
                />
                <label className="form-check-label visually-hidden" htmlFor="es-frecuente">
                  Cliente frecuente
                </label>
              </div>
            </div>
          </section>
        )}

        {paso === 2 && (
          <section>
            <StepHeader
              number={2}
              title="Dirección y validación geográfica"
              description="Usamos OpenStreetMap / Nominatim para normalizar la dirección y ubicar el pin. Si no encontramos la altura exacta, ubicamos la calle o la localidad y el pedido pasa a revisión."
            />

            <div className="address-layout">
              <div className="address-form">
                <div className="row g-3">
                  <div className="col-8">
                    <Field label="Calle" htmlFor="calle" required>
                      <input
                        id="calle"
                        type="text"
                        name="calle"
                        className="form-control"
                        value={direccion.calle}
                        onChange={cambiarDireccion}
                        placeholder="Ej.: Av. Corrientes"
                      />
                    </Field>
                  </div>
                  <div className="col-4">
                    <Field
                      label="Altura"
                      htmlFor="altura"
                      hint="Podés usar S/N si no hay número."
                      error={alturaSinNumero ? 'Altura S/N o 0: el pedido irá a revisión.' : undefined}
                    >
                      <input
                        id="altura"
                        type="text"
                        name="altura"
                        className={`form-control ${alturaSinNumero ? 'is-invalid' : ''}`}
                        value={direccion.altura}
                        onChange={cambiarDireccion}
                        placeholder="1234"
                      />
                    </Field>
                  </div>
                  <div className="col-md-5">
                    <Field label="Piso / Depto" htmlFor="piso-depto">
                      <input
                        id="piso-depto"
                        type="text"
                        name="pisoDepto"
                        className="form-control"
                        value={direccion.pisoDepto}
                        onChange={cambiarDireccion}
                        placeholder="3º B"
                      />
                    </Field>
                  </div>
                  <div className="col-md-7">
                    <Field label="Localidad" htmlFor="localidad" required>
                      <input
                        id="localidad"
                        type="text"
                        name="localidad"
                        className="form-control"
                        value={direccion.localidad}
                        onChange={cambiarDireccion}
                        placeholder="Ej.: CABA"
                      />
                    </Field>
                  </div>
                </div>

                <button
                  type="button"
                  className="btn btn-primary btn-validate"
                  onClick={validarDireccion}
                  disabled={validandoGeo}
                >
                  {validandoGeo ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                      Validando…
                    </>
                  ) : (
                    <>
                      <Icon name="mapPin" size={18} />
                      Validar dirección
                    </>
                  )}
                </button>

                {errorGeo && (
                  <div className="alert alert-warning d-flex align-items-start gap-2 mt-3" role="alert">
                    <Icon name="alert" size={18} />
                    <span>{errorGeo}</span>
                  </div>
                )}

                {geo && !direccionValidada && (
                  <div className="inline-warning">
                    <Icon name="alert" size={17} />
                    La dirección cambió desde la última validación. Volvé a validarla.
                  </div>
                )}

                {geo && direccionValidada && (
                  <div className={`geo-result ${esAmbigua ? 'geo-result--warning' : 'geo-result--success'}`}>
                    <div className="geo-result__head">
                      <span className="geo-result__icon">
                        <Icon name={esAmbigua ? 'alert' : 'checkCircle'} size={19} />
                      </span>
                      <strong>{esAmbigua ? 'Dirección ambigua' : 'Dirección validada'}</strong>
                      <span className="geo-result__badge">
                        {esAmbigua ? 'Requiere revisión' : 'Precisión OK'}
                      </span>
                    </div>
                    <p>{geo.observacion || 'Nominatim encontró la dirección con precisión de puerta.'}</p>
                    <small>{geo.direccionNormalizada}</small>
                    {esAmbigua && (
                      <div className="geo-result__note">
                        El pedido quedará en En revisión para validación manual.
                      </div>
                    )}
                    {pinAjustado && (
                      <div className="geo-result__adjust">
                        <span>
                          Pin corregido manualmente: {Number(pinAjustado.latitud).toFixed(6)},{' '}
                          {Number(pinAjustado.longitud).toFixed(6)}
                        </span>
                        <button
                          type="button"
                          className="geo-result__reset"
                          onClick={() => setPinAjustado(null)}
                        >
                          Restablecer
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="address-map-panel">
                <AddressMap
                  latitud={geoEfectivo?.latitud}
                  longitud={geoEfectivo?.longitud}
                  ajustable={esAmbigua}
                  ajustado={Boolean(pinAjustado)}
                  onAjustar={ajustarPin}
                  direccion={
                    geo?.direccionNormalizada ||
                    `${direccion.calle || 'Dirección'} ${direccion.altura || 'S/N'}, ${
                      direccion.localidad || 'localidad'
                    }`
                  }
                  esEditable
                  onChangeLocation={actualizarUbicacion}
                />
                <div className="map-caption">
                  <Icon name="info" size={15} />
                  OpenStreetMap / Nominatim · sin claves pagas
                </div>
              </div>
            </div>
          </section>
        )}

        {paso === 3 && (
          <section>
            <StepHeader
              number={3}
              title="Detalle del pedido y tarifas"
              description="Describí el paquete y calculamos la tarifa base del envío."
            />

            <div className="row g-3">
              <div className="col-12">
                <Field
                  label="Descripción del paquete"
                  htmlFor="descripcion-paquete"
                  required
                  error={errores.descripcionPaquete}
                >
                  <textarea
                    id="descripcion-paquete"
                    name="descripcionPaquete"
                    rows="3"
                    className={`form-control ${errores.descripcionPaquete ? 'is-invalid' : ''}`}
                    value={detalle.descripcionPaquete}
                    onChange={cambiarDetalle}
                    placeholder="Ej.: Caja de 40x30x20 cm con documentos y muestras."
                  />
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Peso (kg)" htmlFor="peso-kg" required error={errores.pesoKg}>
                  <div className="input-with-suffix">
                    <input
                      id="peso-kg"
                      type="number"
                      inputMode="decimal"
                      min="0.01"
                      max={MAX_PESO_KG}
                      step="0.01"
                      name="pesoKg"
                      className={`form-control ${errores.pesoKg ? 'is-invalid' : ''}`}
                      value={detalle.pesoKg}
                      onChange={cambiarDetalle}
                      placeholder="0,00"
                    />
                    <span>kg</span>
                  </div>
                </Field>
              </div>
              <div className="col-md-6">
                <Field label="Kilómetros" htmlFor="kilometros" required error={errores.kilometros}>
                  <div className="input-with-suffix">
                    <input
                      id="kilometros"
                      type="number"
                      inputMode="decimal"
                      min="0"
                      max={MAX_KILOMETROS}
                      step="0.1"
                      name="kilometros"
                      className={`form-control ${errores.kilometros ? 'is-invalid' : ''}`}
                      value={detalle.kilometros}
                      onChange={cambiarDetalle}
                      placeholder="0,0"
                    />
                    <span>km</span>
                  </div>
                </Field>
              </div>
            </div>

            {tarifa.error ? (
              <div className="alert alert-warning d-flex align-items-start gap-2" role="alert">
                <Icon name="alert" size={18} />
                <span>{tarifa.error}</span>
              </div>
            ) : (
              <div className="price-preview">
                <div className="price-preview__row">
                  <span>Precio base</span>
                  <strong>{formatMoneda(tarifa.precioBase)}</strong>
                </div>
                <div className="price-preview__row">
                  <span>Recargo por excedente</span>
                  <strong>{formatMoneda(tarifa.recargo)}</strong>
                </div>
                <div className="price-preview__row price-preview__row--total">
                  <span>Total estimado</span>
                  <strong>{formatMoneda(tarifa.precioTotal)}</strong>
                </div>
              </div>
            )}

            <p className="tariff-note">
              Tarifa escalonada: {formatMoneda(TARIFA_BASE)} hasta 5 kg y 5 km;{' '}
              {formatMoneda(TARIFA_MEDIA)} si supera 5 kg o 5 km, hasta 20 kg y 15 km;{' '}
              {formatMoneda(RECARGO_POR_KM)} por km extra y {formatMoneda(RECARGO_POR_KG)} por kg
              extra. Máximo {MAX_PESO_KG} kg / {MAX_KILOMETROS} km. El backend recalcula el importe
              al confirmar.
            </p>
          </section>
        )}

        {paso === 4 && (
          <section>
            <StepHeader
              number={4}
              title="Confirmación y envío"
              description="Revisá el resumen antes de registrar el pedido en la base de datos."
            />

            <ResumenPedido
              cliente={cliente}
              direccion={direccion}
              geo={geoEfectivo}
              detalle={detalle}
              tarifa={tarifa}
              prediccion={prediccion}
              pinAjustado={Boolean(pinAjustado)}
            />

            {errorEnvio && (
              <div className="alert alert-danger d-flex align-items-start gap-2 mt-3" role="alert">
                <Icon name="alert" size={18} />
                <span>{errorEnvio}</span>
              </div>
            )}

            <div className="confirm-note">
              <Icon name="shield" size={18} />
              <span>
                Al confirmar, el pedido se registra con la dirección validada y el estado inicial que
                corresponda según las reglas de retención.
              </span>
            </div>
          </section>
        )}
      </div>

      <div className="wizard-footer">
        <button
          type="button"
          className="btn btn-outline-secondary"
          onClick={paso === 1 ? () => navigate('/') : retroceder}
          disabled={enviando}
        >
          <Icon name="arrowLeft" size={17} />
          {paso === 1 ? 'Cancelar' : 'Atrás'}
        </button>

        {paso < 4 ? (
          <button type="button" className="btn btn-primary" onClick={avanzar}>
            Continuar
            <Icon name="arrowRight" size={17} />
          </button>
        ) : (
          <button type="button" className="btn btn-primary" onClick={confirmarPedido} disabled={enviando}>
            {enviando ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                Registrando…
              </>
            ) : (
              <>
                <Icon name="check" size={18} />
                Confirmar y registrar pedido
              </>
            )}
          </button>
        )}
      </div>
    </div>
  )
}
