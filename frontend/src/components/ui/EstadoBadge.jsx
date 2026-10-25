import { getEstadoInfo } from '../../constants/estados'

export default function EstadoBadge({ estado }) {
  const info = getEstadoInfo(estado)

  return (
    <span className={`estado-badge ${info.className}`} title={info.description}>
      <span className="estado-badge__dot" />
      {info.label}
    </span>
  )
}
