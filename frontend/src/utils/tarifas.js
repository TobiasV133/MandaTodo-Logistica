/**
 * Tarifario escalonado replicado desde TarifaService (backend).
 * Si cambian los valores, se deben actualizar ambos lados.
 *
 * Nivel 1: hasta 5 kg y 5 km -> $10.000.
 * Nivel 2: supera 5 kg o 5 km, sin superar 20 kg ni 15 km -> $30.000.
 * Nivel 3: más de 20 kg o más de 15 km -> $30.000 + $5.000 por km extra
 *          + $3.000 por kg extra.
 */
export const TARIFA_BASE = 10000
export const TARIFA_MEDIA = 30000
export const RECARGO_POR_KM = 5000
export const RECARGO_POR_KG = 3000
export const MAX_PESO_KG = 80
export const MAX_KILOMETROS = 30
export const MENSAJE_CAPACIDAD =
  'El pedido excede la capacidad logística máxima (80kg / 30km)'

const LIMITE_PESO_BASE = 5
const LIMITE_KM_BASE = 5
const LIMITE_PESO_MEDIO = 20
const LIMITE_KM_MEDIO = 15

export function calcularTarifa(pesoKg, kilometros) {
  const peso = numero(pesoKg)
  const km = numero(kilometros)

  validarCapacidad(peso, km)

  if (peso <= LIMITE_PESO_BASE && km <= LIMITE_KM_BASE) {
    return resultado(TARIFA_BASE, 0)
  }

  if (peso <= LIMITE_PESO_MEDIO && km <= LIMITE_KM_MEDIO) {
    return resultado(TARIFA_MEDIA, 0)
  }

  const kilometrosExtra = Math.max(km - LIMITE_KM_MEDIO, 0)
  const pesoExtra = Math.max(peso - LIMITE_PESO_MEDIO, 0)
  const recargo = kilometrosExtra * RECARGO_POR_KM + pesoExtra * RECARGO_POR_KG

  return resultado(TARIFA_MEDIA, recargo)
}

export function validarCapacidad(pesoKg, kilometros) {
  const peso = numero(pesoKg)
  const km = numero(kilometros)

  if (peso < 0 || km < 0) {
    throw new Error('El peso y los kilómetros no pueden ser negativos')
  }
  if (peso > MAX_PESO_KG || km > MAX_KILOMETROS) {
    throw new Error(MENSAJE_CAPACIDAD)
  }
}

function numero(valor) {
  if (valor === '' || valor == null) return 0
  const valorNumerico = Number(valor)
  if (!Number.isFinite(valorNumerico)) {
    throw new Error('El peso y los kilómetros deben ser números válidos')
  }
  return valorNumerico
}

function resultado(precioBase, recargo) {
  return {
    precioBase: redondear(precioBase),
    recargo: redondear(recargo),
    precioTotal: redondear(precioBase + recargo),
  }
}

function redondear(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100
}
