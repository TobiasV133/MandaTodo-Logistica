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
 * Detecta el tipo documento a partir de la cantidad de dígitos:
 * 11 dígitos => CUIT; 7 u 8 dígitos => DNI. Permite autocompletar el
 * cliente aunque el usuario haya ingresado el CUIT con el tipo DNI (o viceversa).
 */
export function tipoDocumentoPorDigitos(numeroDocumento) {
  const digitos = soloDigitos(numeroDocumento)
  if (digitos.length === 11) return 'CUIT'
  if (digitos.length >= 7 && digitos.length <= 8) return 'DNI'
  return null
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
