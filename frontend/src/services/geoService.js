import api from './api'

/**
 * @param {{ calle: string, altura: string, localidad: string }} direccion
 * @returns {Promise<{latitud:number, longitud:number, direccionNormalizada:string, esAmbigua:boolean, observacion?:string}>}
 */
export async function normalizarDireccion(direccion) {
  const { data } = await api.post('/geo/normalizar', direccion)
  return data
}
