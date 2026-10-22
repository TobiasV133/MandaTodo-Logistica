import { useNavigate } from 'react-router-dom'

import Icon from '../components/ui/Icon'
import StatCard from '../components/ui/StatCard'
import { useAuth } from '../context/AuthContext'
import { useResumen } from '../context/ResumenContext'

export default function DashboardView() {
  const navigate = useNavigate()
  const { operario } = useAuth()
  const { resumen, loading, error, refresh } = useResumen()

  const primerNombre = operario?.nombreCompleto?.split(' ')[0] ?? 'operario'

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Resumen operativo</span>
          <h2>Hola, {primerNombre}</h2>
          <p>Este es el estado actual de la operación de MandáTodo.</p>
        </div>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={refresh}>
          <Icon name="refresh" size={16} />
          Actualizar
        </button>
      </div>

      {error && (
        <div className="alert alert-warning d-flex align-items-center gap-2" role="alert">
          <Icon name="alert" size={18} />
          {error}
        </div>
      )}

      <div className="metrics-grid">
        <StatCard
          label="Pendientes"
          value={resumen.pendientes}
          icon="package"
          tone="amber"
          hint="Listos para despachar"
          loading={loading}
        />
        <StatCard
          label="En revisión"
          value={resumen.enRevision}
          icon="alert"
          tone="rose"
          hint="Requieren validación"
          loading={loading}
        />
        <StatCard
          label="Entregados"
          value={resumen.entregados}
          icon="checkCircle"
          tone="green"
          hint="Entregas completadas"
          loading={loading}
        />
        <StatCard
          label="Clientes"
          value={resumen.clientes}
          icon="users"
          tone="blue"
          hint="Registrados en el sistema"
          loading={loading}
        />
      </div>

      <div className="dashboard-grid">
        <section className="panel cta-panel">
          <div className="cta-panel__content">
            <span className="cta-panel__eyebrow">Carga de pedidos</span>
            <h2>Registrá una nueva carga en 4 pasos</h2>
            <p>
              Identificá al cliente, validá la dirección en el mapa, completá el detalle
              del paquete y confirmá el envío.
            </p>

            <button
              type="button"
              className="btn btn-light btn-lg cta-panel__button"
              onClick={() => navigate('/nueva-carga')}
            >
              <Icon name="plus" size={19} />
              Registrar nuevo pedido
            </button>
          </div>

          <div className="cta-steps" aria-hidden="true">
            <span><strong>1</strong> Cliente</span>
            <span><strong>2</strong> Dirección</span>
            <span><strong>3</strong> Paquete</span>
            <span><strong>4</strong> Confirmar</span>
          </div>
        </section>

        <section className={`panel review-panel ${resumen.enRevision > 0 ? 'review-panel--attention' : ''}`}>
          <div className="review-panel__head">
            <span className="review-panel__icon">
              <Icon name={resumen.enRevision > 0 ? 'alert' : 'checkCircle'} size={22} />
            </span>
            <div>
              <h3>{resumen.enRevision > 0 ? 'Pedidos retenidos' : 'Bandeja al día'}</h3>
              <p>
                {resumen.enRevision > 0
                  ? 'Hay cargas que necesitan una validación manual antes de despacharse.'
                  : 'No hay pedidos pendientes de revisión en este momento.'}
              </p>
            </div>
          </div>

          <div className="review-panel__number">{resumen.enRevision}</div>
          <span className="review-panel__caption">
            {resumen.enRevision === 1 ? 'pedido en revisión' : 'pedidos en revisión'}
          </span>

          <button
            type="button"
            className={`btn ${resumen.enRevision > 0 ? 'btn-warning' : 'btn-outline-secondary'} w-100`}
            onClick={() => navigate('/en-revision')}
          >
            {resumen.enRevision > 0 ? 'Revisar pedidos' : 'Ver bandeja'}
            <Icon name="arrowRight" size={17} />
          </button>
        </section>
      </div>
    </div>
  )
}
