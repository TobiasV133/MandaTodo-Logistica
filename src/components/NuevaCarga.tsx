import { useState, useEffect, useCallback } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet'
import L from 'leaflet'
import { useApp, generateId, parseAddress, detectAlertas } from '../store'
import type { Cliente, Direccion, Pedido, TipoCliente, TipoDocumento, AlertaRevision } from '../types'

// Fix leaflet default icon
const icon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41],
})

function MapClickHandler({ onMove }: { onMove: (lat: number, lng: number) => void }) {
  useMapEvents({ click(e) { onMove(e.latlng.lat, e.latlng.lng) } })
  return null
}

function MapFlyTo({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap()
  useEffect(() => { map.flyTo([lat, lng], 15, { duration: 1 }) }, [lat, lng])
  return null
}

const STEPS = ['Cliente', 'Dirección', 'Pedido', 'Confirmar'] as const

export default function NuevaCarga() {
  const { clientes, pedidos, addCliente, addPedido, updatePedido, findClienteByDoc, pedidosHoyPorCliente, setView } = useApp()

  const [step, setStep] = useState(0)
  const [submitted, setSubmitted] = useState(false)
  const [resultAlertas, setResultAlertas] = useState<AlertaRevision[]>([])
  const [resultPedidoId, setResultPedidoId] = useState('')
  const [postRemitoOpen, setPostRemitoOpen] = useState(false)

  // Step 1 – Cliente
  const [documento, setDocumento] = useState('')
  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>('DNI')
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [email, setEmail] = useState('')
  const [tipoCliente, setTipoCliente] = useState<TipoCliente>('Particular')
  const [esFrecuente, setEsFrecuente] = useState(false)
  const [clienteExistente, setClienteExistente] = useState<Cliente | null>(null)

  // Step 2 – Dirección
  const [rawDir, setRawDir] = useState('')
  const [parsed, setParsed] = useState<Partial<Direccion & { sinNumero?: boolean }>>({})
  const [calle, setCalle] = useState('')
  const [altura, setAltura] = useState('')
  const [piso, setPiso] = useState('')
  const [depto, setDepto] = useState('')
  const [localidad, setLocalidad] = useState('')
  const [provincia, setProvincia] = useState('Buenos Aires')
  const [lat, setLat] = useState(-34.6037)
  const [lng, setLng] = useState(-58.3816)
  const [pinConfirmado, setPinConfirmado] = useState(false)
  const [geocodingLoading, setGeocodingLoading] = useState(false)
  // confidence: 'alta' | 'media' | 'baja' | null (null = no geocoded yet)
  const [confiabilidad, setConfiabilidad] = useState<'alta' | 'media' | 'baja' | null>(null)
  const [geocodingDone, setGeocodingDone] = useState(false)

  // Step 3 – Pedido
  const [descripcion, setDescripcion] = useState('')
  const [peso, setPeso] = useState('')
  const [monto, setMonto] = useState('')
  const [kilometros, setKilometros] = useState('')
  const [fechaEntrega, setFechaEntrega] = useState('')

  // Suggested tariff remains editable; it is recalculated only when weight or distance changes.
  useEffect(() => {
    const kg = Number.parseFloat(peso.replace(',', '.'))
    const km = Number.parseFloat(kilometros.replace(',', '.'))
    if (Number.isFinite(kg) && Number.isFinite(km)) {
      setMonto(String(Math.round(1500 + kg * 250 + km * 180)))
    }
  }, [peso, kilometros])

  // Autocomplete on doc change
  useEffect(() => {
    if (documento.length >= 7) {
      const found = findClienteByDoc(documento)
      if (found) {
        setClienteExistente(found)
        setNombre(found.nombre)
        setTelefono(found.telefono)
        setEmail(found.email)
        setTipoCliente(found.tipoCliente)
        setEsFrecuente(found.esFrecuente)
      } else {
        setClienteExistente(null)
      }
    } else {
      setClienteExistente(null)
    }
  }, [documento])

  // Parse address on change
  useEffect(() => {
    if (rawDir.length > 5) {
      const p = parseAddress(rawDir)
      setParsed(p)
      setCalle(p.calle || '')
      setAltura(p.altura || '')
      setPiso(p.piso || '')
      setDepto(p.depto || '')
      setLocalidad(p.localidad || '')
    }
  }, [rawDir])

  const geocode = useCallback(async () => {
    const query = [calle, altura, localidad, 'Argentina'].filter(Boolean).join(' ')
    setGeocodingLoading(true)
    setConfiabilidad(null)
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1&countrycodes=ar&addressdetails=1`,
        { headers: { 'Accept-Language': 'es' } }
      )
      const data = await res.json()
      if (data[0]) {
        const r = data[0]
        const addr = r.address ?? {}

        // Normalize fields from Nominatim response
        const normCalle = addr.road ?? addr.pedestrian ?? addr.footway ?? calle
        const normAltura = addr.house_number ?? altura
        const normLocalidad = addr.city ?? addr.town ?? addr.village ?? addr.suburb ?? addr.municipality ?? localidad
        const normProvincia = addr.state ?? provincia

        setCalle(normCalle)
        setAltura(normAltura)
        setLocalidad(normLocalidad)
        setProvincia(normProvincia)
        setLat(parseFloat(r.lat))
        setLng(parseFloat(r.lon))
        setPinConfirmado(false)

        // Score: importance (0–1) + house_number presence + place_rank
        const imp = parseFloat(r.importance ?? '0')
        const hasNumber = !!addr.house_number
        const rank = r.place_rank ?? 30

        let conf: 'alta' | 'media' | 'baja'
        if (imp >= 0.45 && hasNumber && rank <= 30) {
          conf = 'alta'
        } else if (imp >= 0.25 || hasNumber) {
          conf = 'media'
        } else {
          conf = 'baja'
        }
        setConfiabilidad(conf)
      } else {
        setConfiabilidad('baja')
      }
      setGeocodingDone(true)
    } catch {
      setConfiabilidad('baja')
      setGeocodingDone(true)
    }
    setGeocodingLoading(false)
  }, [calle, altura, localidad, provincia])

  function handleSubmit() {
    const esClienteFrecuente = clienteExistente?.esFrecuente ?? esFrecuente
    const alertas = esClienteFrecuente
      ? []
      : detectAlertas({ calle, altura, sinNumero: !altura || altura === '0' })

    // Inject confidence alert
    if (!esClienteFrecuente && confiabilidad === 'baja') alertas.push('fiabilidad_baja')
    else if (!esClienteFrecuente && confiabilidad === 'media') alertas.push('fiabilidad_media')

    // Duplicate detection for Empresa
    let clienteId: string
    let dir: Direccion

    const existing = clienteExistente || findClienteByDoc(documento)
    if (existing) {
      clienteId = existing.id
      // Duplicate detection — only for Empresa clients, checks all active pedidos
      if (existing.tipoCliente === 'Empresa') {
        const activePedidos = pedidos.filter(p =>
          p.clienteId === existing.id &&
          (p.estado === 'pendiente' || p.estado === 'en_revision')
        )
        const dup = activePedidos.find(p => {
          const existingDir = existing.direcciones.find(d => d.id === p.direccionId) ?? existing.direcciones[0]
          const norm = (s?: string) => (s ?? '').toLowerCase().trim()
          return (
            norm(p.descripcion) === norm(descripcion) &&
            norm(p.peso)        === norm(peso) &&
            norm(p.monto)       === norm(monto) &&
            norm(p.kilometros)  === norm(kilometros) &&
            norm(existingDir?.calle)   === norm(calle) &&
            norm(existingDir?.altura)  === norm(altura) &&
            norm(existingDir?.piso)    === norm(piso) &&
            norm(existingDir?.depto)   === norm(depto)
          )
        })
        if (dup) alertas.push('duplicado_empresa')
      }
      dir = existing.direcciones[0] || buildDir()
    } else {
      clienteId = generateId()
      dir = buildDir()
      const newCliente: Cliente = {
        id: clienteId, nombre, documento, tipoDocumento, tipoCliente, esFrecuente,
        telefono, email, creadoEn: new Date().toISOString().split('T')[0],
        direcciones: [dir],
      }
      addCliente(newCliente)
    }

    const pedidoId = 'P-' + generateId()
    const estado: Pedido['estado'] = alertas.length > 0 ? 'en_revision' : 'pendiente'
    const now = new Date().toISOString()
    const newPedido: Pedido = {
      id: pedidoId, clienteId, direccionId: dir.id,
      descripcion, peso, monto, kilometros, fechaEntrega, estado, alertas,
      creadoEn: now.split('T')[0], actualizadoEn: now,
    }
    addPedido(newPedido)
    setResultAlertas(alertas)
    setResultPedidoId(pedidoId)
    setSubmitted(true)
  }

  function buildDir(): Direccion {
    return {
      id: generateId(), raw: rawDir, calle, altura, piso, depto,
      localidad, provincia, lat, lng, confirmada: pinConfirmado,
    }
  }

  if (submitted) {
    const ok = resultAlertas.length === 0
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-10 max-w-md w-full text-center">
          <div className={`w-14 h-14 rounded-full mx-auto mb-5 flex items-center justify-center text-2xl ${ok ? 'bg-[#DCFCE7]' : 'bg-[#FEF3C7]'}`}>
            {ok ? '✓' : '⚠'}
          </div>
          <div className={`text-lg font-semibold mb-2 ${ok ? 'text-[#15803D]' : 'text-[#92400E]'}`}>
            {ok ? 'Pedido registrado' : 'Pedido en revisión'}
          </div>
          <div className="font-mono text-xs text-[#64748B] mb-4">{resultPedidoId}</div>
          {!ok && (
            <div className="text-left space-y-2 mb-6">
              {resultAlertas.map(a => (
                <div key={a} className="flex items-start gap-2 text-xs bg-[#FEF3C7] border border-[#92400E]/20 rounded-lg px-3 py-2 text-[#92400E]">
                  <span className="mt-0.5">⚠</span>
                  <span>{alertaLabel(a)}</span>
                </div>
              ))}
            </div>
          )}
          <p className="text-xs text-[#64748B] mb-6">
            {ok
              ? 'El pedido pasó a la bandeja de Pendientes y puede asignarse a un chofer.'
              : 'El pedido fue retenido en la bandeja de revisión hasta su confirmación.'}
          </p>
          <div className="grid grid-cols-3 gap-2">
            <button
              onClick={() => { setSubmitted(false); setStep(0); setDocumento(''); setNombre(''); setRawDir(''); setDescripcion('') }}
              className="bg-[#F1F5F9] hover:bg-[#E2E8F0] text-[#0F172A] text-xs py-2.5 rounded-lg transition-colors"
            >
              + Nuevo pedido
            </button>
            <button
              onClick={() => {
                updatePedido(resultPedidoId, { remitoGenerado: true })
                setPostRemitoOpen(true)
              }}
              className="border border-[#2563EB]/40 text-[#1D4ED8] hover:bg-[#2563EB]/10 text-xs py-2.5 rounded-lg transition-colors"
            >
              Imprimir Remito
            </button>
            <button
              onClick={() => setView(ok ? 'pedidos' : 'revision')}
              className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs py-2.5 rounded-lg transition-colors"
            >
              {ok ? 'Ver pedidos' : 'Ver revisión'}
            </button>
          </div>
        </div>
        {postRemitoOpen && (
          <div className="fixed inset-0 z-[1100] bg-[#0F172A]/95 overflow-y-auto p-6 text-left">
            <div className="max-w-3xl mx-auto">
              <div className="flex items-center justify-between mb-4">
                <div><div className="text-base font-semibold text-[#0F172A]">Vista previa de remito</div><div className="text-xs text-[#64748B] mt-1">Formato A4 · {resultPedidoId}</div></div>
                <button onClick={() => setPostRemitoOpen(false)} className="text-xs text-[#475569] border border-[#CBD5E1] px-3 py-2 rounded">Cerrar</button>
              </div>
              <div className="bg-white text-slate-900 mx-auto shadow-sm min-h-[820px] max-w-[650px] p-12 relative">
                <div className="flex justify-between items-start border-b-2 border-slate-900 pb-5">
                  <div><div className="text-2xl font-bold">MandáTodo</div><div className="text-xs text-slate-500 mt-1">Logística y distribución</div></div>
                  <div className="text-right"><div className="text-xs uppercase text-slate-500">Remito</div><div className="text-xl font-mono font-bold">{resultPedidoId}</div><div className="text-xs text-slate-500">{new Date().toLocaleDateString('es-AR')}</div></div>
                </div>
                <div className="grid grid-cols-2 gap-8 py-7 border-b border-slate-200">
                  <div><div className="text-[10px] uppercase font-bold text-slate-400 mb-2">Cliente</div><div className="font-semibold">{nombre}</div><div className="text-sm text-slate-600">{tipoDocumento} {documento}</div><div className="text-sm text-slate-600">{email}</div></div>
                  <div><div className="text-[10px] uppercase font-bold text-slate-400 mb-2">Dirección de entrega</div><div className="font-semibold">{calle} {altura}</div><div className="text-sm text-slate-600">{piso ? `Piso ${piso} ` : ''}{depto ? `Dpto ${depto}` : ''}</div><div className="text-sm text-slate-600">{localidad}, {provincia}</div></div>
                </div>
                <table className="w-full mt-8">
                  <thead><tr className="border-b-2 border-slate-800"><th className="text-left text-xs uppercase py-3">Descripción</th><th className="text-right text-xs uppercase py-3">Peso</th><th className="text-right text-xs uppercase py-3">Km</th><th className="text-right text-xs uppercase py-3">Costo</th></tr></thead>
                  <tbody><tr className="border-b border-slate-200"><td className="py-5 text-sm">{descripcion}</td><td className="py-5 text-sm text-right">{peso}</td><td className="py-5 text-sm text-right">{kilometros}</td><td className="py-5 text-sm text-right font-semibold">${monto}</td></tr></tbody>
                </table>
                <div className="mt-32 grid grid-cols-2 gap-16"><div className="border-t border-slate-500 pt-2 text-center text-xs text-slate-500">Firma del receptor</div><div className="border-t border-slate-500 pt-2 text-center text-xs text-slate-500">Aclaración y DNI</div></div>
              </div>
              <div className="flex justify-center mt-4"><button onClick={() => window.print()} className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm px-7 py-3 rounded-md shadow-sm">Imprimir</button></div>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="max-w-3xl mx-auto">

        {/* Header */}
        <div className="mb-6">
          <h2 className="text-base font-semibold text-[#0F172A]">Nueva carga</h2>
          <p className="text-xs text-[#64748B] mt-0.5">Registro de pedido con validación geográfica</p>
        </div>

        {/* Stepper */}
        <div className="flex items-center gap-0 mb-8">
          {STEPS.map((s, i) => (
            <div key={s} className="flex items-center flex-1 last:flex-none">
              <div className={`flex items-center gap-2 ${i <= step ? 'text-[#0F172A]' : 'text-[#64748B]'}`}>
                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-mono border ${
                  i < step ? 'bg-[#15803D] border-[#15803D] text-white'
                  : i === step ? 'bg-[#2563EB] border-[#2563EB] text-white'
                  : 'bg-transparent border-[#E2E8F0] text-[#64748B]'
                }`}>
                  {i < step ? '✓' : i + 1}
                </div>
                <span className="text-xs hidden sm:inline">{s}</span>
              </div>
              {i < STEPS.length - 1 && (
                <div className={`flex-1 h-px mx-3 ${i < step ? 'bg-[#15803D]' : 'bg-[#E2E8F0]'}`} />
              )}
            </div>
          ))}
        </div>

        {/* Steps */}
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">

          {/* STEP 0 — CLIENTE */}
          {step === 0 && (
            <div className="p-6 space-y-5">
              <SectionTitle>Identificación del cliente</SectionTitle>

              <div className="grid grid-cols-3 gap-4">
                <div className="col-span-1">
                  <Label>Tipo de documento</Label>
                  <select value={tipoDocumento} onChange={e => setTipoDocumento(e.target.value as TipoDocumento)} className={inputCls}>
                    <option value="DNI">DNI</option>
                    <option value="CUIT">CUIT</option>
                  </select>
                </div>
                <div className="col-span-2">
                  <Label>Número</Label>
                  <input value={documento} onChange={e => setDocumento(e.target.value)} placeholder="20-12345678-3" className={inputCls} />
                </div>
              </div>

              {clienteExistente && (
                <div className="flex items-start gap-3 bg-[#DCFCE7] border border-[#15803D]/30 rounded-lg px-4 py-3">
                  <span className="text-[#15803D] text-sm mt-0.5">✓</span>
                  <div>
                    <div className="text-xs font-semibold text-[#15803D]">Cliente registrado</div>
                    <div className="text-xs text-[#334155] mt-0.5">{clienteExistente.nombre} · {clienteExistente.tipoCliente}</div>
                    {clienteExistente.esFrecuente && <div className="text-[10px] font-mono text-[#0369A1] mt-1">★ Cliente frecuente</div>}
                  </div>
                </div>
              )}

              <div>
                <Label>Nombre / Razón social</Label>
                <input value={nombre} onChange={e => setNombre(e.target.value)} placeholder="Ej: Distribuidora El Cóndor S.A." className={inputCls} disabled={!!clienteExistente} />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Teléfono</Label>
                  <input value={telefono} onChange={e => setTelefono(e.target.value)} placeholder="011-4555-0000" className={inputCls} disabled={!!clienteExistente} />
                </div>
                <div>
                  <Label>Email</Label>
                  <input value={email} onChange={e => setEmail(e.target.value)} placeholder="contacto@ejemplo.com" className={inputCls} disabled={!!clienteExistente} />
                </div>
              </div>

              {!clienteExistente && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label>Tipo de cliente</Label>
                    <select value={tipoCliente} onChange={e => setTipoCliente(e.target.value as TipoCliente)} className={inputCls}>
                      <option value="Particular">Particular</option>
                      <option value="Empresa">Empresa</option>
                    </select>
                  </div>
                  <div className="flex items-end pb-0.5">
                    <label className="flex items-center gap-2.5 cursor-pointer">
                      <div
                        onClick={() => setEsFrecuente(v => !v)}
                        className={`w-9 h-5 rounded-full transition-colors flex items-center ${esFrecuente ? 'bg-[#2563EB]' : 'bg-[#E2E8F0]'}`}
                      >
                        <div className={`w-4 h-4 rounded-full bg-white shadow transition-transform mx-0.5 ${esFrecuente ? 'translate-x-4' : 'translate-x-0'}`} />
                      </div>
                      <span className="text-xs text-[#334155]">Cliente frecuente</span>
                    </label>
                  </div>
                </div>
              )}

              <div className="flex justify-end pt-2">
                <button
                  onClick={() => setStep(1)}
                  disabled={!documento || !nombre}
                  className="bg-[#2563EB] disabled:opacity-40 hover:bg-[#1D4ED8] text-white text-sm px-6 py-2.5 rounded-lg transition-colors"
                >
                  Siguiente →
                </button>
              </div>
            </div>
          )}

          {/* STEP 1 — DIRECCIÓN */}
          {step === 1 && (
            <div className="p-6 space-y-5">
              <SectionTitle>Dirección de entrega</SectionTitle>

              <div>
                <Label>Dirección completa (ingreso libre)</Label>
                <input
                  value={rawDir}
                  onChange={e => setRawDir(e.target.value)}
                  placeholder="Av. Corrientes 1234 Piso 3 Dpto B, Buenos Aires"
                  className={inputCls}
                />
                <p className="text-[10px] text-[#64748B] mt-1.5">El sistema detectará calle, altura, piso y depto automáticamente.</p>
              </div>

              {/* Parsed fields */}
              {rawDir.length > 5 && (
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="text-[10px] font-mono text-[#64748B] uppercase tracking-wider">Desglose</div>
                    {geocodingDone && confiabilidad && (
                      <ConfBadge conf={confiabilidad} />
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <Label>Calle</Label>
                      <input value={calle} onChange={e => { setCalle(e.target.value); setGeocodingDone(false); setConfiabilidad(null) }} className={inputCls} />
                    </div>
                    <div>
                      <Label>Altura</Label>
                      <input value={altura} onChange={e => { setAltura(e.target.value); setGeocodingDone(false); setConfiabilidad(null) }} placeholder="S/N" className={`${inputCls} ${(!altura || altura === '0') ? 'border-[#92400E]' : ''}`} />
                    </div>
                    <div>
                      <Label>Piso</Label>
                      <input value={piso} onChange={e => setPiso(e.target.value)} placeholder="Opcional" className={inputCls} />
                    </div>
                    <div>
                      <Label>Dpto</Label>
                      <input value={depto} onChange={e => setDepto(e.target.value)} placeholder="Opcional" className={inputCls} />
                    </div>
                    <div>
                      <Label>Localidad</Label>
                      <input value={localidad} onChange={e => { setLocalidad(e.target.value); setGeocodingDone(false); setConfiabilidad(null) }} className={inputCls} />
                    </div>
                    <div>
                      <Label>Provincia</Label>
                      <input value={provincia} onChange={e => setProvincia(e.target.value)} className={inputCls} />
                    </div>
                  </div>

                  {(!altura || altura === '0') && (
                    <div className="mt-3 flex items-start gap-2 text-xs bg-[#FEF3C7] border border-[#92400E]/20 rounded-lg px-3 py-2 text-[#92400E]">
                      <span>⚠</span> Sin número de altura — el pedido irá a revisión.
                    </div>
                  )}

                  <div className="mt-4 flex items-center gap-3 flex-wrap">
                    <button
                      onClick={geocode}
                      disabled={geocodingLoading}
                      className="text-xs bg-[#F1F5F9] hover:bg-[#E2E8F0] border border-[#E2E8F0] text-[#0369A1] px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
                    >
                      {geocodingLoading ? 'Normalizando...' : '🔍 Normalizar y geolocalizar'}
                    </button>
                    {!geocodingDone && !geocodingLoading && (
                      <span className="text-[10px] text-[#64748B]">Recomendado antes de confirmar</span>
                    )}
                    {geocodingDone && confiabilidad === 'alta' && (
                      <span className="text-[10px] text-[#15803D]">✓ Dirección normalizada y verificada</span>
                    )}
                  </div>
                </div>
              )}

              {/* Map */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <Label>Confirmación visual del pin</Label>
                  {pinConfirmado && <span className="text-[10px] font-mono text-[#15803D]">✓ Pin confirmado</span>}
                </div>
                <div className="rounded-lg overflow-hidden border border-[#E2E8F0]" style={{ height: 260 }}>
                  <MapContainer center={[lat, lng]} zoom={13} style={{ height: '100%', width: '100%' }} zoomControl={false}>
                    <TileLayer
                      url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
                      attribution='&copy; Esri'
                    />
                    <Marker position={[lat, lng]} icon={icon} />
                    <MapClickHandler onMove={(la, ln) => { setLat(la); setLng(ln); setPinConfirmado(true) }} />
                    <MapFlyTo lat={lat} lng={lng} />
                  </MapContainer>
                </div>
                <p className="text-[10px] text-[#64748B] mt-1.5">Hacé clic en el mapa para mover el pin y confirmar la ubicación exacta.</p>
              </div>

              <div className="flex justify-between pt-2">
                <button onClick={() => setStep(0)} className="text-sm text-[#64748B] hover:text-[#0F172A] transition-colors">← Atrás</button>
                <button
                  onClick={() => setStep(2)}
                  disabled={!calle}
                  className="bg-[#2563EB] disabled:opacity-40 hover:bg-[#1D4ED8] text-white text-sm px-6 py-2.5 rounded-lg transition-colors"
                >
                  Siguiente →
                </button>
              </div>
            </div>
          )}

          {/* STEP 2 — PEDIDO */}
          {step === 2 && (
            <div className="p-6 space-y-5">
              <SectionTitle>Detalle del pedido</SectionTitle>

              <div>
                <Label>Descripción</Label>
                <input value={descripcion} onChange={e => setDescripcion(e.target.value)} placeholder="Ej: Caja de electrónica — 3 unidades" className={inputCls} />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Peso estimado</Label>
                  <input value={peso} onChange={e => setPeso(e.target.value)} placeholder="Ej: 5 kg" className={inputCls} />
                </div>
                <div>
                  <Label>Precio sugerido ($) · editable</Label>
                  <input value={monto} onChange={e => setMonto(e.target.value)} placeholder="Ej: 25000" className={inputCls} />
                  <div className="text-[10px] text-[#64748B] mt-1">Base $1.500 + $250/kg + $180/km. Podés ajustar el total manualmente.</div>
                </div>
                <div>
                  <Label>Kilómetros</Label>
                  <input value={kilometros} onChange={e => setKilometros(e.target.value)} placeholder="Ej: 45" className={inputCls} />
                </div>
                <div>
                  <Label>Fecha de entrega</Label>
                  <input type="date" value={fechaEntrega} onChange={e => setFechaEntrega(e.target.value)} className={inputCls} />
                </div>
              </div>

              <div className="flex justify-between pt-2">
                <button onClick={() => setStep(1)} className="text-sm text-[#64748B] hover:text-[#0F172A] transition-colors">← Atrás</button>
                <button
                  onClick={() => setStep(3)}
                  disabled={!descripcion}
                  className="bg-[#2563EB] disabled:opacity-40 hover:bg-[#1D4ED8] text-white text-sm px-6 py-2.5 rounded-lg transition-colors"
                >
                  Siguiente →
                </button>
              </div>
            </div>
          )}

          {/* STEP 3 — CONFIRMACIÓN */}
          {step === 3 && (
            <div className="p-6 space-y-5">
              <SectionTitle>Confirmar y registrar</SectionTitle>

              <div className="space-y-3">
                <ReviewRow label="Cliente" value={nombre} />
                <ReviewRow label="Documento" value={`${tipoDocumento} ${documento}`} />
                <ReviewRow label="Tipo" value={`${clienteExistente?.tipoCliente || tipoCliente} · ${(clienteExistente?.esFrecuente || esFrecuente) ? 'Frecuente ★' : 'Estándar'}`} />
                <ReviewRow label="Dirección" value={`${calle} ${altura}${piso ? `, Piso ${piso}` : ''}${depto ? ` Dpto ${depto}` : ''}, ${localidad}`} />
                <ReviewRow label="Pin" value={pinConfirmado ? `${lat.toFixed(5)}, ${lng.toFixed(5)} ✓` : 'Sin confirmar'} />
                <ReviewRow label="Pedido" value={descripcion} />
                <ReviewRow label="Peso" value={peso || '—'} />
                <ReviewRow label="Monto" value={monto ? `$${monto}` : '—'} />
                <ReviewRow label="Kilómetros" value={kilometros ? `${kilometros} km` : '—'} />
                <ReviewRow label="Fecha entrega" value={fechaEntrega || '—'} />
              </div>

              {/* Pre-flight alerts */}
              {(() => {
                const esClienteFrecuente = clienteExistente?.esFrecuente ?? esFrecuente
                const preAlertas = esClienteFrecuente
                  ? []
                  : detectAlertas({ calle, altura, sinNumero: !altura || altura === '0' })
                if (!esClienteFrecuente && confiabilidad === 'baja') preAlertas.push('fiabilidad_baja')
                else if (!esClienteFrecuente && confiabilidad === 'media') preAlertas.push('fiabilidad_media')

                // Duplicate check — Empresa only, same as handleSubmit
                const existingForCheck = clienteExistente || findClienteByDoc(documento)
                if (existingForCheck?.tipoCliente === 'Empresa') {
                  const norm = (s?: string) => (s ?? '').toLowerCase().trim()
                  const activePedidos = pedidos.filter(p =>
                    p.clienteId === existingForCheck.id &&
                    (p.estado === 'pendiente' || p.estado === 'en_revision')
                  )
                  const dup = activePedidos.find(p => {
                    const existingDir = existingForCheck.direcciones.find(d => d.id === p.direccionId) ?? existingForCheck.direcciones[0]
                    return (
                      norm(p.descripcion) === norm(descripcion) &&
                      norm(p.peso)        === norm(peso) &&
                      norm(p.monto)       === norm(monto) &&
                      norm(p.kilometros)  === norm(kilometros) &&
                      norm(existingDir?.calle)  === norm(calle) &&
                      norm(existingDir?.altura) === norm(altura) &&
                      norm(existingDir?.piso)   === norm(piso) &&
                      norm(existingDir?.depto)  === norm(depto)
                    )
                  })
                  if (dup && !preAlertas.includes('duplicado_empresa')) preAlertas.push('duplicado_empresa')
                }

                if (preAlertas.length === 0 && esClienteFrecuente) return (
                  <div className="flex items-center gap-2 text-xs text-[#15803D] bg-[#DCFCE7] border border-[#15803D]/20 rounded-lg px-4 py-3">
                    <span>✓</span> Cliente frecuente — se omite la alerta de dirección dudosa y el pedido pasará a Pendiente.
                  </div>
                )
                if (preAlertas.length === 0 && confiabilidad === 'alta') return (
                  <div className="flex items-center gap-2 text-xs text-[#15803D] bg-[#DCFCE7] border border-[#15803D]/20 rounded-lg px-4 py-3">
                    <span>✓</span> Dirección verificada con fiabilidad alta — el pedido pasará directo a Pendiente.
                  </div>
                )
                if (preAlertas.length === 0 && !confiabilidad) return (
                  <div className="flex items-center gap-2 text-xs text-[#92400E] bg-[#FEF3C7] border border-[#92400E]/20 rounded-lg px-4 py-3">
                    <span>⚠</span> No se geolocalizo la dirección — se recomienda normalizar antes de registrar.
                  </div>
                )
                return (
                  <div className="space-y-2">
                    {preAlertas.map(a => (
                      <div key={a} className={`flex items-start gap-2 text-xs rounded-lg px-4 py-3 border ${a === 'fiabilidad_baja' || a === 'duplicado_empresa' ? 'bg-[#FEE2E2] border-[#B91C1C]/20 text-[#B91C1C]' : 'bg-[#FEF3C7] border-[#92400E]/20 text-[#92400E]'}`}>
                        <span>⚠</span> {alertaLabel(a)} — el pedido irá a revisión.
                      </div>
                    ))}
                  </div>
                )
              })()}

              <div className="flex justify-between pt-2">
                <button onClick={() => setStep(2)} className="text-sm text-[#64748B] hover:text-[#0F172A] transition-colors">← Atrás</button>
                <button
                  onClick={handleSubmit}
                  className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm px-6 py-2.5 rounded-lg transition-colors"
                >
                  Registrar pedido
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="text-sm font-semibold text-[#0F172A] pb-1 border-b border-[#E2E8F0]">{children}</h3>
}
function Label({ children }: { children: React.ReactNode }) {
  return <label className="block text-xs font-medium text-[#64748B] mb-1.5">{children}</label>
}
function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2 border-b border-[#E2E8F0] last:border-0">
      <span className="text-xs text-[#64748B] w-24 flex-shrink-0">{label}</span>
      <span className="text-xs text-[#334155] text-right">{value}</span>
    </div>
  )
}

const inputCls = 'w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2 text-sm text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors disabled:opacity-50'

function ConfBadge({ conf }: { conf: 'alta' | 'media' | 'baja' }) {
  const map = {
    alta:  { label: 'Fiabilidad alta',  cls: 'bg-[#DCFCE7] text-[#15803D] border-[#15803D]/30' },
    media: { label: 'Fiabilidad media', cls: 'bg-[#FEF3C7] text-[#92400E] border-[#92400E]/30' },
    baja:  { label: 'Fiabilidad baja',  cls: 'bg-[#FEE2E2] text-[#B91C1C] border-[#B91C1C]/30' },
  }
  const { label, cls } = map[conf]
  return <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${cls}`}>{label}</span>
}

function alertaLabel(a: AlertaRevision): string {
  switch (a) {
    case 'geo_ambigua':      return 'Dirección geográficamente ambigua o incompleta.'
    case 'altura_cero':      return 'Altura en cero, S/N o sin número de puerta detectado.'
    case 'duplicado_empresa':return 'Sospecha de pedido duplicado — cliente Empresa con pedido activo idéntico.'
    case 'sin_numero':       return 'Sin número de altura identificado.'
    case 'fiabilidad_baja':  return 'Geocodificación sin resultado confiable — dirección no verificada.'
    case 'fiabilidad_media': return 'Dirección aproximada sin número de puerta confirmado por el geocodificador.'
  }
}
