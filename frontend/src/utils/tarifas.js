/**
 * Tarifario escalonado replicado desde TarifaService (backend).
 *
 * Los valores por defecto coinciden con TarifaValores.POR_DEFECTO. Los valores
 * vigentes se obtienen de GET /api/tarifas y los administradores pueden editarlos
 * desde la pantalla de pedidos.
 *
 * Nivel 1: hasta limitePesoBase kg y limiteKmBase km -> tarifaBase.
 * Nivel 2: hasta limitePesoMedio kg y limiteKmMedio km -> tarifaMedia.
 * Nivel 3: supera el nivel medio -> tarifaMedia + recargoPorKm por km extra
 *          + recargoPorKg por kg extra.
 */
export const TARIFA_POR_DEFECTO = Object.freeze({
  tarifaBase: 10000,
  tarifaMedia: 30000,
  recargoPorKm: 5000,
  recargoPorKg: 3000,
  limitePesoBase: 5,
  limiteKmBase: 5,
  limitePesoMedio: 20,
  limiteKmMedio: 15,
  maxPesoKg: 80,
  maxKilometros: 30,
})

const CAMPOS = Object.keys(TARIFA_POR_DEFECTO)

/**
 * Toma los valores que llegan de la API (o de un objeto parcial) y completa con
 * los valores por defecto. Descarta números inválidos o negativos.
 */
export function normalizarTarifa(valores) {
  const resultado = { ...TARIFA_POR_DEFECTO }
  if (!valores || typeof valores !== 'object') return resultado

  CAMPOS.forEach((campo) => {
    const numero = Number(valores[campo])
    if (Number.isFinite(numero) && numero >= 0) {
      resultado[campo] = numero
    }
  })

  return resultado
}

export function calcularTarifa(pesoKg, kilometros, tarifa = TARIFA_POR_DEFECTO) {
  const config = normalizarTarifa(tarifa)
  const peso = numero(pesoKg)
  const km = numero(kilometros)

  validarCapacidad(peso, km, config)

  if (peso <= config.limitePesoBase && km <= config.limiteKmBase) {
    return resultado(config.tarifaBase, 0)
  }

  if (peso <= config.limitePesoMedio && km <= config.limiteKmMedio) {
    return resultado(config.tarifaMedia, 0)
  }

  const kilometrosExtra = Math.max(km - config.limiteKmMedio, 0)
  const pesoExtra = Math.max(peso - config.limitePesoMedio, 0)
  const recargo =
    kilometrosExtra * config.recargoPorKm + pesoExtra * config.recargoPorKg

  return resultado(config.tarifaMedia, recargo)
}

export function validarCapacidad(pesoKg, kilometros, tarifa = TARIFA_POR_DEFECTO) {
  const config = normalizarTarifa(tarifa)
  const peso = numero(pesoKg)
  const km = numero(kilometros)

  if (peso < 0 || km < 0) {
    throw new Error('El peso y los kilómetros no pueden ser negativos')
  }
  if (peso > config.maxPesoKg || km > config.maxKilometros) {
    throw new Error(mensajeCapacidad(config))
  }
}

export function mensajeCapacidad(tarifa = TARIFA_POR_DEFECTO) {
  const config = normalizarTarifa(tarifa)
  return `El pedido excede la capacidad logística máxima (${formatearNumero(
    config.maxPesoKg,
  )}kg / ${formatearNumero(config.maxKilometros)}km)`
}

function formatearNumero(valor) {
  return String(valor)
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
