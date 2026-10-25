export const ESTADOS = {
  PENDIENTE: {
    label: 'Pendiente',
    className: 'estado--pendiente',
    description: 'Listo para despachar',
  },
  EN_REVISION: {
    label: 'En revisión',
    className: 'estado--revision',
    description: 'Requiere validación manual',
  },
  EN_TRANSITO: {
    label: 'En tránsito',
    className: 'estado--transito',
    description: 'En camino al destino',
  },
  CANCELADO_EN_TRANSITO: {
    label: 'Cancelado en tránsito',
    className: 'estado--neutral',
    description: 'Cancelado durante el recorrido',
  },
  ENTREGADO: {
    label: 'Entregado',
    className: 'estado--entregado',
    description: 'Entrega completada',
  },
  FALLIDO: {
    label: 'Fallido',
    className: 'estado--fallido',
    description: 'La entrega no se pudo completar',
  },
}

export const ESTADOS_LISTA = Object.entries(ESTADOS).map(([value, info]) => ({
  value,
  ...info,
}))

export function getEstadoInfo(estado) {
  return (
    ESTADOS[estado] ?? {
      label: estado || 'Sin estado',
      className: 'estado--neutral',
      description: '',
    }
  )
}
