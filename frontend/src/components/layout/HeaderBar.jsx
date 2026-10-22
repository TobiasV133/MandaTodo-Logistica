import { useNavigate } from 'react-router-dom'

import Icon from '../ui/Icon'
import { useResumen } from '../../context/ResumenContext'
import { formatFechaLarga } from '../../utils/formatters'

export default function HeaderBar({ title, onOpenMenu }) {
  const navigate = useNavigate()
  const { resumen } = useResumen()
  const enRevision = resumen.enRevision ?? 0

  return (
    <header className="header-bar">
      <button className="icon-btn d-lg-none" type="button" onClick={onOpenMenu} aria-label="Abrir menú">
        <Icon name="menu" size={22} />
      </button>

      <div className="header-title">
        <span className="header-kicker">Operación · MandáTodo</span>
        <h1>{title}</h1>
      </div>

      <div className="header-actions">
        <span className="header-date">
          <Icon name="calendar" size={16} />
          {formatFechaLarga()}
        </span>

        <button className="btn-review" type="button" onClick={() => navigate('/en-revision')}>
          <Icon name="alert" size={17} />
          <span className="btn-review__count">{enRevision}</span>
          <span className="btn-review__label">en revisión</span>
        </button>

        <button className="btn btn-primary btn-header-cta" type="button" onClick={() => navigate('/nueva-carga')}>
          <Icon name="plus" size={17} />
          Nueva carga
        </button>
      </div>
    </header>
  )
}
