export type EstadoPedidoApi =
  | 'PENDIENTE'
  | 'EN_REVISION'
  | 'EN_TRANSITO'
  | 'CANCELADO_EN_TRANSITO'
  | 'ENTREGADO'
  | 'FALLIDO'

export type MotivoFallaApi =
  | 'Ausente'
  | 'Dirección Inexistente'
  | 'Dirección Incompleta'

export interface RevisionResumenApi {
  idRevision: number
  motivoRetencion: string
  estadoResolucion: string
}

export interface PedidoApi {
  idPedido: number
  idCliente: number
  cliente: string
  tipoCliente: 'Empresa' | 'Particular'
  descripcion: string
  pesoKg: number
  montoEnvio: number
  estado: EstadoPedidoApi
  motivoFalla?: MotivoFallaApi | null
  motivoCancelacion?: string | null
  calle: string
  altura: number | null
  pisoDepto?: string | null
  localidad: string
  latitud?: number | null
  longitud?: number | null
  fechaCreacion: string
  fechaProgramada: string
  revisiones: RevisionResumenApi[]
}

export interface CambioEstadoResponseApi {
  pedido: PedidoApi
  reprogramado: boolean
  recargoAplicado: number | null
  fechaReprogramada: string | null
}

export interface CambiarEstadoRequestApi {
  estado: EstadoPedidoApi
  operarioId: number
  motivoFalla?: MotivoFallaApi
  notaFalla?: string
  motivoCancelacion?: string
}

export interface ImprimirRemitoRequestApi {
  operarioId: number
  motivoReimpresion?: string
  kilometros?: number
}

export class ApiRequestError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  .replace(/\/+$/, '')

export function getOperarioId(): number {
  const value = import.meta.env.VITE_OPERARIO_ID?.trim()
  const id = Number(value)

  if (!value || !Number.isSafeInteger(id) || id <= 0) {
    throw new Error('Configurá VITE_OPERARIO_ID con el ID de un operario activo para registrar acciones.')
  }

  return id
}

function extractMessage(payload: unknown): string | undefined {
  if (typeof payload === 'string' && payload.trim()) return payload.trim()
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return undefined

  const body = payload as Record<string, unknown>
  for (const key of ['detail', 'message', 'error', 'title']) {
    if (typeof body[key] === 'string' && body[key].trim()) return body[key].trim()
  }

  const errors = body.errors
  if (Array.isArray(errors)) {
    const messages = errors.flatMap(error => {
      if (!error || typeof error !== 'object') return []
      const item = error as Record<string, unknown>
      return typeof item.defaultMessage === 'string' ? [item.defaultMessage] : []
    })
    if (messages.length) return messages.join(' ')
  }

  return undefined
}

async function throwResponseError(response: Response): Promise<never> {
  const text = await response.text()
  let payload: unknown = text

  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = text
    }
  }

  throw new ApiRequestError(
    extractMessage(payload) ?? `La solicitud al servidor falló (HTTP ${response.status}).`,
    response.status,
  )
}

async function fetchApi(input: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiRequestError(
      `No se pudo conectar con el backend en ${API_BASE_URL}. Verificá que esté en ejecución.`,
      0,
    )
  }
}

export async function listarPedidos(signal?: AbortSignal): Promise<PedidoApi[]> {
  const response = await fetchApi(`${API_BASE_URL}/api/pedidos`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) return throwResponseError(response)
  return response.json() as Promise<PedidoApi[]>
}

export async function cambiarEstadoPedido(
  id: number,
  request: CambiarEstadoRequestApi,
): Promise<CambioEstadoResponseApi> {
  const response = await fetchApi(`${API_BASE_URL}/api/pedidos/${id}/estado`, {
    method: 'PATCH',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) return throwResponseError(response)
  return response.json() as Promise<CambioEstadoResponseApi>
}

export async function imprimirRemito(
  id: number,
  request: ImprimirRemitoRequestApi,
): Promise<Blob> {
  const response = await fetchApi(`${API_BASE_URL}/api/remitos/${id}/imprimir`, {
    method: 'POST',
    headers: {
      // Aceptamos JSON además del PDF para poder leer los errores 409/422 del backend.
      Accept: 'application/pdf, application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) return throwResponseError(response)
  return response.blob()
}
