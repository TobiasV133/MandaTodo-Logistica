import Icon from '../ui/Icon'
import {
  formatMoneda,
  formatPeso,
} from '../../utils/formatters'
import { formatearDocumento } from '../../utils/documentos'

function SummaryRow({ label, value, strong = false }) {
  return (
    <div className="summary-row">
      <span className="summary-row__label">{label}</span>
      <span className={`summary-row__value ${strong ? 'summary-row__value--strong' : ''}`}>
        {value || '—'}
      </span>
    </div>
  )
}

export default function ResumenPedido({
  cliente,
  direccion,
  geo,
  detalle,
  tarifa,
  prediccion,
  pinAjustado = false,
}) {
  const direccionTexto = `${direccion.calle || '—'} ${direccion.altura || 'S/N'}${
    direccion.pisoDepto ? `, ${direccion.pisoDepto}` : ''
  }, ${direccion.localidad || '—'}`

  return (
    <div className="summary-layout">
      <div className="summary-main">
        <section className="summary-block">
          <div className="summary-block__head">
            <span className="summary-block__icon"><Icon name="user" size={18} /></span>
            <h3>Cliente</h3>
          </div>
          <SummaryRow
            label="Documento"
            value={`${cliente.tipoDocumento} ${formatearDocumento(cliente.tipoDocumento, cliente.numeroDocumento)}`}
          />
          <SummaryRow label="Nombre / razón social" value={cliente.nombreRazonSocial} />
          <SummaryRow label="Email" value={cliente.email} />
          <SummaryRow label="Teléfono" value={cliente.telefono} />
          <SummaryRow label="Tipo de cliente" value={cliente.tipoCliente} />
          <SummaryRow
            label="Cliente frecuente"
            value={cliente.esFrecuente ? 'Sí' : 'No'}
          />
        </section>

        <section className="summary-block">
          <div className="summary-block__head">
            <span className="summary-block__icon"><Icon name="mapPin" size={18} /></span>
            <h3>Dirección</h3>
          </div>
          <SummaryRow label="Dirección" value={direccionTexto} />
          {geo?.direccionNormalizada && (
            <SummaryRow label="Normalizada por OSM" value={geo.direccionNormalizada} />
          )}
          <SummaryRow
            label="Coordenadas"
            value={
              geo?.latitud != null && geo?.longitud != null
                ? `${Number(geo.latitud).toFixed(6)}, ${Number(geo.longitud).toFixed(6)}`
                : null
            }
          />
          <SummaryRow
            label="Validación geográfica"
            value={geo?.esAmbigua ? 'Ambigua · requiere revisión' : 'Validada'}
          />
          {pinAjustado && (
            <SummaryRow label="Pin del mapa" value="Corregido manualmente" />
          )}
        </section>

        <section className="summary-block">
          <div className="summary-block__head">
            <span className="summary-block__icon"><Icon name="package" size={18} /></span>
            <h3>Paquete</h3>
          </div>
          <SummaryRow label="Descripción" value={detalle.descripcionPaquete} />
          <SummaryRow label="Peso" value={`${formatPeso(detalle.pesoKg)} kg`} />
          <SummaryRow label="Kilómetros" value={`${formatPeso(detalle.kilometros)} km`} />
        </section>
      </div>

      <aside className="summary-aside">
        <div className="price-panel">
          <span className="price-panel__eyebrow">Tarifa estimada</span>
          {tarifa.error ? (
            <p className="text-danger mb-0">{tarifa.error}</p>
          ) : (
            <>
              <div className="price-panel__total">{formatMoneda(tarifa.precioTotal)}</div>
              <div className="price-panel__line">
                <span>Precio base</span>
                <strong>{formatMoneda(tarifa.precioBase)}</strong>
              </div>
              <div className="price-panel__line">
                <span>Recargo por excedente</span>
                <strong>{formatMoneda(tarifa.recargo)}</strong>
              </div>
              <p className="price-panel__note">
                El backend recalcula la tarifa al confirmar el pedido.
              </p>
            </>
          )}
        </div>

        <div className={`prediction-card prediction-card--${prediccion.tone}`}>
          <span className="prediction-card__icon">
            <Icon name={prediccion.icon} size={19} />
          </span>
          <div>
            <strong>{prediccion.title}</strong>
            <p>{prediccion.detail}</p>
          </div>
        </div>
      </aside>
    </div>
  )
}
