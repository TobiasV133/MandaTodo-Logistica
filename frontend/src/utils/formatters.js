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

export function formatFechaLarga(value = new Date()) {
  const fecha = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(fecha.getTime())) return '—'
  return `${fecha.getDate()} de ${MESES_CORTOS[fecha.getMonth()]} de ${fecha.getFullYear()}`
}

export function formatFecha(value) {
  if (!value) return '—'
  const fecha = new Date(value)
  if (Number.isNaN(fecha.getTime())) return '—'
  return formatFechaLarga(fecha)
}

export function formatFechaHora(value) {
  if (!value) return '—'
  const fecha = new Date(value)
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
  if (!nombre) return 'MT'
  return nombre
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join('')
}

export function capitalizar(texto) {
  if (!texto) return ''
  return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase()
}
