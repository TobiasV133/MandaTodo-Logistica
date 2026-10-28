import { useCallback, useEffect, useMemo } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'

import Icon from '../ui/Icon'

const CENTRO_CABA = [-34.6037, -58.3816]

function MapFocus({ latitud, longitud }) {
  const map = useMap()

  useEffect(() => {
    if (Number.isFinite(latitud) && Number.isFinite(longitud)) {
      map.flyTo([latitud, longitud], 16, { duration: 0.8 })
    }
  }, [map, latitud, longitud])

  return null
}

/**
 * Mapa reutilizable de OpenStreetMap / React-Leaflet.
 *
 * @param {object} props
 * @param {number|string|null|undefined} props.latitud - Latitud inicial o controlada.
 * @param {number|string|null|undefined} props.longitud - Longitud inicial o controlada.
 * @param {string} [props.direccion] - Texto mostrado en el popup del marcador.
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
  esEditable = true,
  readOnly = false,
  onChangeLocation,
  onPositionChange,
}) {
  const editable = esEditable && !readOnly
  const notificarCambio = onChangeLocation ?? onPositionChange

  const handleDragEnd = useCallback(
    (event) => {
      if (!editable || typeof notificarCambio !== 'function') return

      const { lat, lng } = event.target.getLatLng()
      notificarCambio({ latitud: lat, longitud: lng })
    },
    [editable, notificarCambio],
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
        />
        {tieneCoordenadas && (
          <Marker
            position={posicion}
            draggable={editable}
            eventHandlers={eventHandlers}
          >
            <Popup>{direccion || 'Ubicación validada'}</Popup>
          </Marker>
        )}
      </MapContainer>

      {!tieneCoordenadas && (
        <div className="map-empty-note">
          <Icon name="mapPin" size={16} />
          Validá la dirección para ubicar el pin en el mapa.
        </div>
      )}
    </div>
  )
}
