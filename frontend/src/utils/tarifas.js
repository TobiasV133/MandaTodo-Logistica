/**
 * Tarifario replicado desde TarifaService (backend).
 * Si cambian los valores, se deben actualizar ambos lados.
 *
 * precioBase = cargo fijo + (km * valorKm) + (kg * valorKg)
 */
export const CARGO_FIJO = 2500
export const VALOR_POR_KM = 450
export const VALOR_POR_KG = 180

export function calcularTarifa(pesoKg, kilometros) {
  const peso = Number(pesoKg) || 0
  const km = Number(kilometros) || 0
  const precioBase = CARGO_FIJO + km * VALOR_POR_KM + peso * VALOR_POR_KG
  const recargo = 0
  const precioTotal = precioBase + recargo

  return {
    precioBase: redondear(precioBase),
    recargo: redondear(recargo),
    precioTotal: redondear(precioTotal),
  }
}

function redondear(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100
}
