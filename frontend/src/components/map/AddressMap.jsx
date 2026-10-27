import { useEffect } from 'react'
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

export default function AddressMap({
  latitud,
  longitud,
  direccion,
  ajustable = false,
  ajustado = false,
  onAjustar,
}) {
  const tieneCoordenadas =
    latitud !== null &&
    latitud !== undefined &&
    longitud !== null &&
    longitud !== undefined &&
    Number.isFinite(Number(latitud)) &&
    Number.isFinite(Number(longitud))

  const posicion = tieneCoordenadas ? [Number(latitud), Number(longitud)] : CENTRO_CABA

  const moverPin = (nuevaLatitud, nuevaLongitud) => {
    if (typeof onAjustar === 'function') {
      onAjustar(nuevaLatitud, nuevaLongitud)
    }
  }

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
        <MapClickHandler activo={ajustable && tieneCoordenadas} onElegir={moverPin} />
        {tieneCoordenadas && (
          <Marker
            position={posicion}
            draggable={ajustable}
            eventHandlers={{
              dragend: (evento) => {
                const punto = evento.target.getLatLng()
                moverPin(punto.lat, punto.lng)
              },
            }}
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
