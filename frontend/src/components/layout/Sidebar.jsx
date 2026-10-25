import { NavLink, useNavigate } from 'react-router-dom'

import Icon from '../ui/Icon'
import { useAuth } from '../../context/AuthContext'
import { useResumen } from '../../context/ResumenContext'
import { iniciales } from '../../utils/formatters'

const NAV_ITEMS = [
  { to: '/', label: 'Panel', icon: 'dashboard', end: true },
  { to: '/nueva-carga', label: 'Nueva carga', icon: 'plus' },
  { to: '/pedidos', label: 'Pedidos', icon: 'package' },
  { to: '/en-revision', label: 'En revisión', icon: 'alert', badgeKey: 'enRevision' },
  { to: '/clientes', label: 'Clientes', icon: 'users' },
  { to: '/usuarios', label: 'Usuarios', icon: 'shield' },
  { to: '/auditoria', label: 'Auditoría', icon: 'history' },
]

export default function Sidebar({ isOpen, onClose }) {
  const navigate = useNavigate()
  const { operario, logout } = useAuth()
  const { resumen } = useResumen()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <aside className={`sidebar ${isOpen ? 'sidebar--open' : ''}`}>
      <div className="sidebar__top">
        <NavLink to="/" className="sidebar-brand" onClick={onClose} aria-label="MandáTodo - Panel">
          <span className="brand-mark">
            <Icon name="package" size={22} strokeWidth={1.9} />
          </span>
          <span className="brand-copy">
            <span className="brand-name">
              Mandá<span>Todo</span>
            </span>
            <span className="brand-tagline">Logística urbana</span>
          </span>
        </NavLink>

        <button className="sidebar-close d-lg-none" type="button" onClick={onClose} aria-label="Cerrar menú">
          <Icon name="x" size={20} />
        </button>
      </div>

      <div className="sidebar-section-label">Operación</div>

      <nav className="sidebar-nav" aria-label="Navegación principal">
        {NAV_ITEMS.map((item) => {
          const badge = item.badgeKey ? resumen[item.badgeKey] : 0
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-item-custom ${isActive ? 'active' : ''}`}
              onClick={onClose}
            >
              <span className="nav-item-icon">
                <Icon name={item.icon} size={19} />
              </span>
              <span className="nav-item-label">{item.label}</span>
              {item.badgeKey && badge > 0 && <span className="nav-badge">{badge}</span>}
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">
          <span className="avatar">{iniciales(operario?.nombreCompleto)}</span>
          <span className="sidebar-user__copy">
            <span className="sidebar-user__name">{operario?.nombreCompleto ?? 'Operario'}</span>
            <span className="sidebar-user__role">
              {operario?.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Operario'}
            </span>
          </span>
        </div>
        <button type="button" className="sidebar-logout" onClick={handleLogout}>
          <Icon name="logout" size={18} />
          Cerrar sesión
        </button>
        <div className="sidebar-legal">Sistema interno · MandáTodo</div>
      </div>
    </aside>
  )
}
