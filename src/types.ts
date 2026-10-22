export type TipoCliente = 'Particular' | 'Empresa'
export type TipoDocumento = 'DNI' | 'CUIT'
export type EstadoPedido = 'en_revision' | 'pendiente' | 'en_transito' | 'cancelado_transito' | 'entregado' | 'fallido'

export const MOTIVOS_FALLA = [
  'Ausente',
  'Dirección Inexistente',
  'Dirección Incompleta',
  'Rehúsa la entrega',
  'Fuera de horario',
  'Acceso restringido',
  'Otro',
] as const

export type MotivoFalla = typeof MOTIVOS_FALLA[number]

export type AlertaRevision =
  | 'geo_ambigua'
  | 'altura_cero'
  | 'duplicado_empresa'
  | 'sin_numero'
  | 'fiabilidad_baja'
  | 'fiabilidad_media'

export interface Direccion {
  id: string
  raw: string
  calle: string
  altura: string
  piso: string
  depto: string
  localidad: string
  provincia: string
  lat?: number
  lng?: number
  confirmada: boolean
}

export interface Cliente {
  id: string
  nombre: string
  documento: string
  tipoDocumento: TipoDocumento
  tipoCliente: TipoCliente
  esFrecuente: boolean
  telefono: string
  email: string
  direcciones: Direccion[]
  creadoEn: string
}

export interface Pedido {
  id: string
  clienteId: string
  direccionId: string
  descripcion: string
  peso: string
  monto: string
  kilometros: string
  fechaEntrega: string
  estado: EstadoPedido
  alertas: AlertaRevision[]
  motivoFalla?: MotivoFalla
  notaFalla?: string
  hojaRutaGenerada?: boolean
  remitoGenerado?: boolean
  recargoReprogramacion?: string
  creadoEn: string
  actualizadoEn: string
}
