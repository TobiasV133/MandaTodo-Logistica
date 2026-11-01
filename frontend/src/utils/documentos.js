export const LONGITUD_MAXIMA_DOCUMENTO = 11

export const TIPOS_DOCUMENTO = [
  { value: 'DNI', label: 'DNI' },
  { value: 'CUIT', label: 'CUIT' },
]

export const TIPOS_CLIENTE = [
  { value: 'Particular', label: 'Particular' },
  { value: 'Empresa', label: 'Empresa' },
]

export function soloDigitos(valor) {
  return String(valor ?? '').replace(/\D/g, '')
}

export function longitudDocumento(tipoDocumento) {
  return tipoDocumento === 'CUIT' ? 11 : 8
}

export function documentoCompleto(tipoDocumento, numeroDocumento) {
  const digitos = soloDigitos(numeroDocumento)
  if (tipoDocumento === 'CUIT') return digitos.length === 11
  return digitos.length === 7 || digitos.length === 8
}

/**
 * Resuelve el tipo de documento mientras se escribe sin pisar la selección
 * del usuario: nunca degrada un CUIT a DNI (eso truncaba el número a 8 dígitos
 * mientras se tipeaba) y solo promueve a CUIT cuando se completan los 11 dígitos.
 */
export function tipoDocumentoAlEscribir(tipoActual, numeroDocumento) {
  if (tipoActual === 'CUIT') return 'CUIT'
  return soloDigitos(numeroDocumento).length === 11 ? 'CUIT' : tipoActual || 'DNI'
}

export function validarDocumento(tipoDocumento, numeroDocumento) {
  const digitos = soloDigitos(numeroDocumento)
  if (!digitos) return 'Ingresá el número de documento.'
  if (tipoDocumento === 'CUIT' && digitos.length !== 11) {
    return 'El CUIT debe tener 11 dígitos.'
  }
  if (tipoDocumento === 'DNI' && (digitos.length < 7 || digitos.length > 8)) {
    return 'El DNI debe tener 7 u 8 dígitos.'
  }
  return null
}

export function formatearDocumento(tipoDocumento, numeroDocumento) {
  const digitos = soloDigitos(numeroDocumento)
  if (tipoDocumento === 'CUIT' && digitos.length === 11) {
    return `${digitos.slice(0, 2)}-${digitos.slice(2, 10)}-${digitos.slice(10)}`
  }
  return digitos
}
