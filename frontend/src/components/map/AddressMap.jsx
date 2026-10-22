import { useEffect } from 'react'
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

export default function AddressMap({ latitud, longitud, direccion }) {
  const tieneCoordenadas =
    latitud !== null &&
    latitud !== undefined &&
    longitud !== null &&
    longitud !== undefined &&
    Number.isFinite(Number(latitud)) &&
    Number.isFinite(Number(longitud))

  const posicion = tieneCoordenadas ? [Number(latitud), Number(longitud)] : CENTRO_CABA

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
          <Marker position={posicion}>
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
