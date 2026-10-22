import { useState, useEffect } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet'
import L from 'leaflet'
import { useApp } from '../store'
import type { AlertaRevision } from '../types'

const markerIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41],
})

function alertaBadge(a: AlertaRevision) {
  const map: Record<AlertaRevision, { label: string; desc: string; color: string }> = {
    geo_ambigua:       { label: 'Dirección ambigua',       desc: 'La calle o localidad no pudo verificarse con certeza.',                                            color: 'bg-[#E2E8F0] text-[#92400E] border-[#92400E]/20' },
    altura_cero:       { label: 'Altura S/N o cero',       desc: 'No se detectó número de puerta. Confirmar con el remitente antes de despachar.',                  color: 'bg-[#FEF3C7] text-[#92400E] border-[#92400E]/20' },
    duplicado_empresa: { label: 'Sospecha de pedido duplicado', desc: 'Cliente Empresa con pedido activo idéntico en dirección, descripción, peso, monto y km. Verificar antes de despachar.', color: 'bg-[#FEE2E2] text-[#B91C1C] border-[#B91C1C]/20' },
    sin_numero:        { label: 'Sin número detectado',    desc: 'No se identificó altura en la dirección ingresada.',                                               color: 'bg-[#FEF3C7] text-[#92400E] border-[#92400E]/20' },
    fiabilidad_baja:   { label: 'Fiabilidad baja',         desc: 'La geocodificación no encontró la dirección con certeza. Confirmar en el mapa antes de despachar.', color: 'bg-[#FEE2E2] text-[#B91C1C] border-[#B91C1C]/20' },
    fiabilidad_media:  { label: 'Fiabilidad media',        desc: 'La dirección fue aproximada pero sin número de puerta confirmado. Verificar pin en el mapa.',      color: 'bg-[#FEF3C7] text-[#92400E] border-[#92400E]/20' },
  }
  return map[a]
}

function MapFlyTo({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap()
  useEffect(() => { map.flyTo([lat, lng], 15, { duration: 0.8 }) }, [lat, lng])
  return null
}

function PinMover({ onMove }: { onMove: (lat: number, lng: number) => void }) {
  useMapEvents({ click(e) { onMove(e.latlng.lat, e.latlng.lng) } })
  return null
}

interface PinState { lat: number; lng: number; moved: boolean }

function AddressMap({ clienteId, direccionId }: { clienteId: string; direccionId: string }) {
  const { clientes } = useApp()
  const cliente = clientes.find(c => c.id === clienteId)
  const dir = cliente?.direcciones.find(d => d.id === direccionId) ?? cliente?.direcciones[0]

  const [calle, setCalle] = useState(dir?.calle ?? '')
  const [altura, setAltura] = useState(dir?.altura ?? '')
  const [piso, setPiso] = useState(dir?.piso ?? '')
  const [depto, setDepto] = useState(dir?.depto ?? '')
  const [localidad, setLocalidad] = useState(dir?.localidad ?? '')
  const [provincia, setProvincia] = useState(dir?.provincia ?? '')

  const [pin, setPin] = useState<PinState>({
    lat: dir?.lat ?? -34.6037,
    lng: dir?.lng ?? -58.3816,
    moved: false,
  })
  const [geocoding, setGeocoding] = useState(false)

  async function geocode() {
    const q = [calle, altura, localidad, 'Argentina'].filter(Boolean).join(' ')
    setGeocoding(true)
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(q)}&format=json&limit=1&countrycodes=ar&addressdetails=1`,
        { headers: { 'Accept-Language': 'es' } }
      )
      const data = await res.json()
      if (data[0]) {
        const r = data[0]
        const addr = r.address ?? {}
        if (addr.road)         setCalle(addr.road)
        if (addr.house_number) setAltura(addr.house_number)
        if (addr.city || addr.town || addr.village) setLocalidad(addr.city ?? addr.town ?? addr.village)
        if (addr.state) setProvincia(addr.state)
        setPin({ lat: parseFloat(r.lat), lng: parseFloat(r.lon), moved: false })
      }
    } catch {}
    setGeocoding(false)
  }

  if (!dir) return <div className="text-xs text-[#64748B]">Sin dirección registrada.</div>

  const inputCls = 'w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded px-2.5 py-1.5 text-xs text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors'

  return (
    <div className="space-y-3">
      {/* Editable address fields */}
      <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg p-4">
        <div className="flex items-center justify-between mb-3">
          <span className="text-[10px] font-mono text-[#64748B] uppercase tracking-wider">Dirección — editable</span>
          <button
            onClick={geocode}
            disabled={geocoding}
            className="text-[10px] bg-[#F1F5F9] hover:bg-[#E2E8F0] border border-[#E2E8F0] text-[#0369A1] px-2.5 py-1 rounded transition-colors disabled:opacity-50"
          >
            {geocoding ? 'Normalizando...' : '🔍 Normalizar y geolocalizar'}
          </button>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div className="col-span-2">
            <label className="block text-[10px] text-[#64748B] mb-1">Calle</label>
            <input value={calle} onChange={e => setCalle(e.target.value)} className={`${inputCls} ${!calle ? 'border-[#92400E]' : ''}`} />
          </div>
          <div>
            <label className="block text-[10px] text-[#64748B] mb-1">Altura</label>
            <input value={altura} onChange={e => setAltura(e.target.value)} placeholder="S/N" className={`${inputCls} ${!altura || altura === '0' ? 'border-[#92400E]' : ''}`} />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[10px] text-[#64748B] mb-1">Piso</label>
              <input value={piso} onChange={e => setPiso(e.target.value)} placeholder="—" className={inputCls} />
            </div>
            <div>
              <label className="block text-[10px] text-[#64748B] mb-1">Dpto</label>
              <input value={depto} onChange={e => setDepto(e.target.value)} placeholder="—" className={inputCls} />
            </div>
          </div>
          <div>
            <label className="block text-[10px] text-[#64748B] mb-1">Localidad</label>
            <input value={localidad} onChange={e => setLocalidad(e.target.value)} className={`${inputCls} ${!localidad ? 'border-[#92400E]' : ''}`} />
          </div>
          <div>
            <label className="block text-[10px] text-[#64748B] mb-1">Provincia</label>
            <input value={provincia} onChange={e => setProvincia(e.target.value)} className={inputCls} />
          </div>
        </div>
      </div>

      {/* Map */}
      <div>
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-[10px] font-mono text-[#64748B] uppercase tracking-wider">
            Mapa — hacé clic para mover el pin
          </span>
          {pin.moved && (
            <span className="text-[10px] font-mono text-[#15803D]">
              ✓ {pin.lat.toFixed(5)}, {pin.lng.toFixed(5)}
            </span>
          )}
        </div>
        <div className="rounded-lg overflow-hidden border border-[#E2E8F0]" style={{ height: 220 }}>
          <MapContainer center={[pin.lat, pin.lng]} zoom={14} style={{ height: '100%', width: '100%' }} zoomControl={false}>
            <TileLayer
              url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
              attribution='&copy; Esri'
            />
            <Marker position={[pin.lat, pin.lng]} icon={markerIcon} />
            <PinMover onMove={(la, ln) => setPin({ lat: la, lng: ln, moved: true })} />
            <MapFlyTo lat={pin.lat} lng={pin.lng} />
          </MapContainer>
        </div>
      </div>
    </div>
  )
}

export default function BandejaRevision() {
  const { pedidos, clientes, aprobarRevision, rechazarRevision } = useApp()
  const [expanded, setExpanded] = useState<string | null>(null)
  const [confirming, setConfirming] = useState<{ id: string; action: 'aprobar' | 'rechazar' } | null>(null)

  const enRevision = pedidos.filter(p => p.estado === 'en_revision')

  function confirm() {
    if (!confirming) return
    if (confirming.action === 'aprobar') aprobarRevision(confirming.id)
    else rechazarRevision(confirming.id)
    setConfirming(null)
    setExpanded(null)
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-base font-semibold text-[#0F172A]">Bandeja — En Revisión</h2>
            <p className="text-xs text-[#64748B] mt-0.5">Revisá la dirección y el pin antes de aprobar o rechazar cada pedido</p>
          </div>
          <div className="bg-[#FEF3C7] border border-[#92400E]/30 text-[#92400E] font-mono text-sm px-3 py-1.5 rounded-lg">
            {enRevision.length} pendiente{enRevision.length !== 1 ? 's' : ''}
          </div>
        </div>

        {enRevision.length === 0 ? (
          <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-12 text-center">
            <div className="text-2xl mb-3">✓</div>
            <div className="text-sm text-[#15803D] font-medium">Bandeja limpia</div>
            <div className="text-xs text-[#64748B] mt-1">No hay pedidos retenidos en este momento.</div>
          </div>
        ) : (
          <div className="space-y-3">
            {enRevision.map(p => {
              const cliente = clientes.find(c => c.id === p.clienteId)
              const isExpanded = expanded === p.id
              const hasGeoAlerta = true // always show address editor for all revision items

              return (
                <div key={p.id} className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">
                  {/* Header row — always visible */}
                  <button
                    onClick={() => setExpanded(isExpanded ? null : p.id)}
                    className="w-full text-left px-5 py-4 hover:bg-[#F1F5F9] transition-colors"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1 flex-wrap">
                          <span className="font-mono text-xs text-[#0369A1]">{p.id}</span>
                          <span className="text-[10px] font-mono text-[#64748B]">{p.creadoEn}</span>
                          {p.alertas.map(a => {
                            const b = alertaBadge(a)
                            return (
                              <span key={a} className={`text-[9px] font-mono px-1.5 py-0.5 rounded border ${b.color}`}>
                                ⚠ {b.label}
                              </span>
                            )
                          })}
                        </div>
                        <div className="text-sm text-[#0F172A]">{p.descripcion}</div>
                        {cliente && (
                          <div className="text-xs text-[#64748B] mt-0.5">
                            {cliente.nombre}
                            {cliente.tipoCliente === 'Empresa' && <span className="ml-1.5 text-[#2563EB]">[Empresa]</span>}
                          </div>
                        )}
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        {p.monto && <span className="font-mono text-xs text-[#0F172A]">${p.monto}</span>}
                        <span className={`text-[10px] text-[#64748B] transition-transform ${isExpanded ? 'rotate-180' : ''}`}>▼</span>
                      </div>
                    </div>
                  </button>

                  {/* Expanded detail */}
                  {isExpanded && (
                    <div className="px-5 pb-5 border-t border-[#E2E8F0] pt-4 space-y-4">

                      {/* Alert details */}
                      <div className="space-y-2">
                        {p.alertas.map(a => {
                          const b = alertaBadge(a)
                          return (
                            <div key={a} className={`flex items-start gap-2 text-xs rounded-lg px-3 py-2 border ${b.color}`}>
                              <span className="mt-0.5 flex-shrink-0">⚠</span>
                              <div>
                                <span className="font-semibold">{b.label}</span>
                                <span className="opacity-80 ml-1.5">{b.desc}</span>
                              </div>
                            </div>
                          )
                        })}
                      </div>

                      {/* Address + map for geographic alerts */}
                      {hasGeoAlerta && (
                        <AddressMap
                          clienteId={p.clienteId}
                          direccionId={p.direccionId}
                        />
                      )}

                      {/* Actions */}
                      {confirming?.id === p.id ? (
                        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg p-4 flex items-center justify-between gap-4">
                          <span className="text-xs text-[#334155]">
                            {confirming.action === 'aprobar'
                              ? '¿Confirmar aprobación y pasar a Pendiente?'
                              : '¿Rechazar y marcar como Fallido?'}
                          </span>
                          <div className="flex gap-2">
                            <button onClick={() => setConfirming(null)} className="text-xs text-[#64748B] hover:text-[#0F172A] px-3 py-1.5 rounded transition-colors">
                              Cancelar
                            </button>
                            <button
                              onClick={confirm}
                              className={`text-xs text-white px-4 py-1.5 rounded transition-colors ${confirming.action === 'aprobar' ? 'bg-[#15803D] hover:bg-[#166534]' : 'bg-[#B91C1C] hover:bg-[#991B1B]'}`}
                            >
                              Confirmar
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex gap-2 pt-1">
                          <button
                            onClick={() => setConfirming({ id: p.id, action: 'aprobar' })}
                            className="flex-1 bg-[#DCFCE7] hover:bg-[#BBF7D0] border border-[#15803D]/30 text-[#15803D] text-xs py-2.5 rounded-lg transition-colors"
                          >
                            ✓ Aprobar — pasar a Pendiente
                          </button>
                          <button
                            onClick={() => setConfirming({ id: p.id, action: 'rechazar' })}
                            className="flex-1 bg-[#FEE2E2] hover:bg-[#FECACA] border border-[#B91C1C]/30 text-[#B91C1C] text-xs py-2.5 rounded-lg transition-colors"
                          >
                            ✕ Rechazar — marcar Fallido
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
