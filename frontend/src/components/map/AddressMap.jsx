import { useCallback, useEffect, useMemo } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'

import Icon from '../ui/Icon'

const CENTRO_CABA = [-34.6037, -58.3816]

function MapFocus({ latitud, longitud, seguir }) {
  const map = useMap()

  useEffect(() => {
    if (seguir && Number.isFinite(latitud) && Number.isFinite(longitud)) {
      map.flyTo([latitud, longitud], 16, { duration: 0.8 })
    }
  }, [map, latitud, longitud, seguir])

  return null
}

function MapClickHandler({ activo, onElegir }) {
  useMapEvents({
    click(evento) {
      if (!activo) return
      onElegir(evento.latlng.lat, evento.latlng.lng)
    },
  })

  return null
}

/**
 * Mapa reutilizable de OpenStreetMap / React-Leaflet.
 *
 * Acepta dos APIs de props equivalentes, para no romper a las vistas que
 * todavia usan la firma anterior:
 *  - `ajustable` / `ajustado` / `onAjustar(latitud, longitud)`
 *  - `esEditable` / `readOnly` / `onChangeLocation({ latitud, longitud })` u `onPositionChange`
 *
 * @param {object} props
 * @param {number|string|null|undefined} props.latitud - Latitud inicial o controlada.
 * @param {number|string|null|undefined} props.longitud - Longitud inicial o controlada.
 * @param {string} [props.direccion] - Texto mostrado en el popup del marcador.
 * @param {boolean} [props.ajustable=false] - Habilita ajustar el pin con clic en el mapa.
 * @param {boolean} [props.ajustado=false] - Indica que el pin ya fue corregido a mano.
 * @param {(latitud: number, longitud: number) => void} [props.onAjustar] - Callback de ajuste con coordenadas sueltas.
 * @param {boolean} [props.esEditable=true] - Si es true, el marcador se puede arrastrar.
 * @param {boolean} [props.readOnly=false] - Alias inverso de `esEditable`; si es true, fuerza el modo lectura.
 * @param {(coordenadas: { latitud: number, longitud: number }) => void} [props.onChangeLocation]
 *   Callback invocado al soltar el marcador (`dragend`).
 * @param {(coordenadas: { latitud: number, longitud: number }) => void} [props.onPositionChange]
 *   Alias de `onChangeLocation` para mantener compatibilidad entre vistas.
 */
export default function AddressMap({
  latitud,
  longitud,
  direccion,
  ajustable = false,
  ajustado = false,
  onAjustar,
  esEditable = true,
  readOnly = false,
  onChangeLocation,
  onPositionChange,
}) {
  const editable = esEditable && !readOnly
  const notificarCambio = onChangeLocation ?? onPositionChange

  // Un solo punto de salida: prioriza `onAjustar` (firma antigua con lat/lng)
  // y cae a las props nuevas que reciben un objeto de coordenadas.
  const notificarAjuste = useCallback(
    (nuevaLatitud, nuevaLongitud) => {
      if (typeof onAjustar === 'function') {
        onAjustar(nuevaLatitud, nuevaLongitud)
        return
      }
      if (typeof notificarCambio === 'function') {
        notificarCambio({ latitud: nuevaLatitud, longitud: nuevaLongitud })
      }
    },
    [onAjustar, notificarCambio],
  )

  const handleDragEnd = useCallback(
    (evento) => {
      if (!editable) return

      const { lat, lng } = evento.target.getLatLng()
      notificarAjuste(lat, lng)
    },
    [editable, notificarAjuste],
  )

  const tieneCoordenadas =
    latitud !== null &&
    latitud !== undefined &&
    longitud !== null &&
    longitud !== undefined &&
    Number.isFinite(Number(latitud)) &&
    Number.isFinite(Number(longitud))

  const posicion = useMemo(
    () => (tieneCoordenadas ? [Number(latitud), Number(longitud)] : CENTRO_CABA),
    [tieneCoordenadas, latitud, longitud],
  )

  const eventHandlers = useMemo(
    () => (editable ? { dragend: handleDragEnd } : undefined),
    [editable, handleDragEnd],
  )

  return (
    <div className="map-shell">
      <MapContainer
        center={posicion}
        zoom={tieneCoordenadas ? 16 : 12}
        scrollWheelZoom={false}
        className="map-canvas"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapFocus
          latitud={tieneCoordenadas ? Number(latitud) : null}
          longitud={tieneCoordenadas ? Number(longitud) : null}
          seguir={!ajustado}
        />
        <MapClickHandler activo={ajustable && tieneCoordenadas} onElegir={notificarAjuste} />
        {tieneCoordenadas && (
          <Marker
            position={posicion}
            draggable={editable}
            eventHandlers={eventHandlers}
          >
            <Popup>
              {ajustado
                ? 'Ubicación corregida manualmente'
                : direccion || 'Ubicación validada'}
            </Popup>
          </Marker>
        )}
      </MapContainer>

      {!tieneCoordenadas && (
        <div className="map-empty-note">
          <Icon name="mapPin" size={16} />
          Validá la dirección para ubicar el pin en el mapa.
        </div>
      )}

      {ajustable && tieneCoordenadas && (
        <div className="map-adjust-note">
          <Icon name="mapPin" size={15} />
          Arrastrá el pin o hacé clic en el mapa para ajustar la ubicación exacta.
        </div>
      )}
    </div>
  )
}
