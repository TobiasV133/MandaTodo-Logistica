import Icon from '../components/ui/Icon'

export default function AuditoriaView() {
  return (
    <div className="page-stack">
      <div className="page-heading">
        <div>
          <span className="page-eyebrow">Trazabilidad</span>
          <h2>Auditoría</h2>
          <p>Registro de cambios y acciones realizadas por los operarios.</p>
        </div>
      </div>

      <section className="panel module-placeholder">
        <span className="module-placeholder__icon">
          <Icon name="history" size={24} />
        </span>
        <div>
          <h3>Módulo de auditoría</h3>
          <p>
            El historial detallado de modificaciones se implementará en una próxima entrega. Por
            ahora, cada pedido conserva el operario y la fecha de su última modificación.
          </p>
        </div>
      </section>
    </div>
  )
}
