import Icon from './Icon'
import { formatNumero } from '../../utils/formatters'

export default function StatCard({
  label,
  value,
  icon,
  tone = 'blue',
  hint,
  loading = false,
  onClick,
  ariaLabel,
}) {
  const interactive = typeof onClick === 'function'
  const className = `metric-card metric-card--${tone}${interactive ? ' metric-card--interactive' : ''}`

  const contenido = (
    <>
      <span className="metric-card__top">
        <span className="metric-card__icon">
          <Icon name={icon} size={20} />
        </span>
        {hint && <span className="metric-card__hint">{hint}</span>}
      </span>
      <span className="metric-card__value">
        {loading ? <span className="skeleton skeleton-value" /> : formatNumero(value)}
      </span>
      <span className="metric-card__label">{label}</span>
    </>
  )

  if (interactive) {
    return (
      <button
        type="button"
        className={className}
        onClick={onClick}
        aria-label={ariaLabel || `Ver ${label.toLowerCase()}`}
      >
        {contenido}
      </button>
    )
  }

  return <article className={className}>{contenido}</article>
}
