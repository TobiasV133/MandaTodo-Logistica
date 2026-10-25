import Icon from './Icon'
import { formatNumero } from '../../utils/formatters'

export default function StatCard({ label, value, icon, tone = 'blue', hint, loading = false }) {
  return (
    <article className={`metric-card metric-card--${tone}`}>
      <div className="metric-card__top">
        <span className="metric-card__icon">
          <Icon name={icon} size={20} />
        </span>
        {hint && <span className="metric-card__hint">{hint}</span>}
      </div>
      <div className="metric-card__value">
        {loading ? <span className="skeleton skeleton-value" /> : formatNumero(value)}
      </div>
      <div className="metric-card__label">{label}</div>
    </article>
  )
}
