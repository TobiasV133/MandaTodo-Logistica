const MESES_CORTOS = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
]

const FECHA_SIN_HORA = /^\d{4}-\d{2}-\d{2}$/

/**
 * Convierte un valor de fecha en Date sin corrimientos de zona horaria.
 * Las fechas sin hora (YYYY-MM-DD) se interpretan como fecha local: `new Date`
 * las toma como UTC y en Argentina (UTC-3) mostrarían el día anterior.
 */
export function parseFecha(value) {
  if (value instanceof Date) return value
  if (typeof value === 'string' && FECHA_SIN_HORA.test(value.trim())) {
    const [anio, mes, dia] = value.trim().split('-').map(Number)
    return new Date(anio, mes - 1, dia)
  }
  return new Date(value)
}

export function formatFechaLarga(value = new Date()) {
  const fecha = parseFecha(value)
  if (Number.isNaN(fecha.getTime())) return '—'
  return `${fecha.getDate()} de ${MESES_CORTOS[fecha.getMonth()]} de ${fecha.getFullYear()}`
}

export function formatFecha(value) {
  if (!value) return '—'
  const fecha = parseFecha(value)
  if (Number.isNaN(fecha.getTime())) return '—'
  return formatFechaLarga(fecha)
}

/**
 * Formatea una fecha de calendario (YYYY-MM-DD) sin convertirla a UTC.
 * `new Date('2026-11-01')` se interpreta como medianoche UTC y en Argentina
 * retrocede un día; por eso se arma la fecha con sus componentes locales.
 */
export function formatFechaProgramada(value) {
  if (!value) return '—'
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [anio, mes, dia] = value.split('-').map(Number)
    if (!anio || !mes || !dia) return '—'
    return `${dia} de ${MESES_CORTOS[mes - 1]} de ${anio}`
  }
  return formatFecha(value)
}

export function formatFechaHora(value) {
  if (!value) return '—'
  const fecha = parseFecha(value)
  if (Number.isNaN(fecha.getTime())) return '—'
  const dia = String(fecha.getDate()).padStart(2, '0')
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const hora = String(fecha.getHours()).padStart(2, '0')
  const minutos = String(fecha.getMinutes()).padStart(2, '0')
  return `${dia}/${mes}/${fecha.getFullYear()} ${hora}:${minutos}`
}

export function formatMoneda(value) {
  const numero = Number(value ?? 0)
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(numero) ? numero : 0)
}

export function formatNumero(value) {
  const numero = Number(value ?? 0)
  return new Intl.NumberFormat('es-AR').format(Number.isFinite(numero) ? numero : 0)
}

export function formatPeso(value) {
  const numero = Number(value ?? 0)
  return new Intl.NumberFormat('es-AR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(Number.isFinite(numero) ? numero : 0)
}

export function iniciales(nombre) {
  if (typeof nombre !== 'string') return 'MT'

  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return 'MT'
  if (partes.length === 1) return partes[0].charAt(0).toUpperCase()

  return `${partes[0].charAt(0)}${partes[partes.length - 1].charAt(0)}`.toUpperCase()
}

export function capitalizar(texto) {
  if (!texto) return ''
  return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase()
}
