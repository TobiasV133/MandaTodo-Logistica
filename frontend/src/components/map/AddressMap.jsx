import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  useMap,
  useMapEvents,
} from 'react-leaflet'

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
 * Acepta dos APIs de props equivalentes para no romper a las vistas que
 * todavía usan la firma anterior:
 *  - `ajustable` / `ajustado` / `onAjustar(latitud, longitud)`
 *  - `esEditable` / `readOnly` / `onChangeLocation({ latitud, longitud })`
 *    u `onPositionChange`.
 *
 * @param {object} props
 * @param {number|string|null|undefined} props.latitud
 * @param {number|string|null|undefined} props.longitud
 * @param {string} [props.direccion]
 * @param {boolean} [props.ajustable=false]
 * @param {boolean} [props.ajustado=false]
 * @param {(latitud: number, longitud: number) => void} [props.onAjustar]
 * @param {boolean} [props.esEditable=true]
 * @param {boolean} [props.readOnly=false]
 * @param {(coordenadas: { latitud: number, longitud: number }) => void} [props.onChangeLocation]
 * @param {(coordenadas: { latitud: number, longitud: number }) => void} [props.onPositionChange]
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
  const [ajustadoLocal, setAjustadoLocal] = useState(false)
  const pinAjustado = ajustado || ajustadoLocal
  const puedeAjustar = ajustable || editable

  const notificarAjuste = useCallback(
    (nuevaLatitud, nuevaLongitud) => {
      setAjustadoLocal(true)

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
          seguir={!pinAjustado}
        />
        <MapClickHandler
          activo={puedeAjustar && tieneCoordenadas}
          onElegir={notificarAjuste}
        />
        {tieneCoordenadas && (
          <Marker
            position={posicion}
            draggable={editable}
            eventHandlers={eventHandlers}
          >
            <Popup>
              {pinAjustado
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

      {puedeAjustar && tieneCoordenadas && (
        <div className="map-adjust-note">
          <Icon name="mapPin" size={15} />
          {editable
            ? 'Arrastrá el pin o hacé clic en el mapa para ajustar la ubicación exacta.'
            : 'Hacé clic en el mapa para ajustar la ubicación exacta.'}
        </div>
      )}
    </div>
  )
}
