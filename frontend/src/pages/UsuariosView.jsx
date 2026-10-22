import Icon from '../components/ui/Icon'
import { useAuth } from '../context/AuthContext'
import { iniciales } from '../utils/formatters'

export default function UsuariosView() {
  const { operario } = useAuth()

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Seguridad</span>
          <h2>Usuarios</h2>
          <p>Operarios habilitados para acceder al sistema interno.</p>
        </div>
      </div>

      <section className="panel profile-panel">
        <span className="profile-avatar">{iniciales(operario?.nombreCompleto)}</span>
        <div className="profile-copy">
          <h3>{operario?.nombreCompleto}</h3>
          <span className="chip chip--blue">{operario?.rol}</span>
          <dl className="profile-meta">
            <div>
              <dt>Usuario</dt>
              <dd>{operario?.username}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{operario?.email}</dd>
            </div>
          </dl>
        </div>
      </section>

      <section className="panel module-placeholder">
        <span className="module-placeholder__icon">
          <Icon name="shield" size={24} />
        </span>
        <div>
          <h3>Administración de usuarios</h3>
          <p>
            El alta, baja y cambio de rol de operarios corresponde al módulo de seguridad y queda
            fuera del alcance de esta entrega de Carga y Autenticación.
          </p>
        </div>
      </section>
    </div>
  )
}
