import { createContext, useContext, useState, ReactNode } from 'react'
import type { Cliente, Pedido, Direccion, AlertaRevision, TipoCliente, TipoDocumento } from './types'

const mockClientes: Cliente[] = [
  {
    id: 'c1',
    nombre: 'Distribuidora El Cóndor S.A.',
    documento: '30-71234567-1',
    tipoDocumento: 'CUIT',
    tipoCliente: 'Empresa',
    esFrecuente: true,
    telefono: '011-4555-1234',
    email: 'compras@condor.com.ar',
    creadoEn: '2024-03-10',
    direcciones: [{
      id: 'd1', raw: 'Av. San Martín 1450, Buenos Aires',
      calle: 'Av. San Martín', altura: '1450', piso: '', depto: '',
      localidad: 'Buenos Aires', provincia: 'Buenos Aires',
      lat: -34.6037, lng: -58.3816, confirmada: true,
    }],
  },
  {
    id: 'c2',
    nombre: 'Ramírez, Jorge Luis',
    documento: '28456789',
    tipoDocumento: 'DNI',
    tipoCliente: 'Particular',
    esFrecuente: false,
    telefono: '0351-456-7890',
    email: 'jorge.ramirez@gmail.com',
    creadoEn: '2024-06-22',
    direcciones: [{
      id: 'd2', raw: 'Bv. Chacabuco 900 Piso 2 Dpto A, Córdoba',
      calle: 'Bv. Chacabuco', altura: '900', piso: '2', depto: 'A',
      localidad: 'Córdoba', provincia: 'Córdoba',
      lat: -31.4135, lng: -64.1811, confirmada: true,
    }],
  },
]

const today = new Date().toISOString().split('T')[0]
const mockPedidos: Pedido[] = [
  {
    id: 'p1', clienteId: 'c1', direccionId: 'd1',
    descripcion: 'Caja electrónica — 3 unidades', peso: '12 kg', monto: '45000',
    estado: 'pendiente', alertas: [], kilometros: '120', fechaEntrega: today, hojaRutaGenerada: true, creadoEn: today, actualizadoEn: today,
  },
  {
    id: 'p2', clienteId: 'c2', direccionId: 'd2',
    descripcion: 'Ropa deportiva — bolsa grande', peso: '4 kg', monto: '18500', kilometros: '45', fechaEntrega: today,
    estado: 'en_revision', alertas: ['geo_ambigua'], creadoEn: today, actualizadoEn: today,
  },
  {
    id: 'p3', clienteId: 'c1', direccionId: 'd1',
    descripcion: 'Repuestos industriales', peso: '30 kg', monto: '92000', kilometros: '280', fechaEntrega: today,
    estado: 'fallido', alertas: [], motivoFalla: 'Ausente', creadoEn: today, actualizadoEn: today,
  },
]

export type View = 'dashboard' | 'nueva_carga' | 'libreta' | 'revision' | 'pedidos' | 'clientes' | 'usuarios' | 'auditoria'

interface AppCtx {
  view: View
  setView: (v: View) => void
  clientes: Cliente[]
  pedidos: Pedido[]
  addCliente: (c: Cliente) => void
  addPedido: (p: Pedido) => void
  updatePedidoEstado: (id: string, estado: Pedido['estado'], motivo?: Pedido['motivoFalla'], nota?: string) => void
  updatePedido: (id: string, fields: Partial<Pick<Pedido, 'clienteId' | 'descripcion' | 'peso' | 'monto' | 'kilometros' | 'fechaEntrega' | 'hojaRutaGenerada' | 'remitoGenerado' | 'recargoReprogramacion'>>) => void
  updateCliente: (id: string, fields: Partial<Pick<Cliente, 'nombre' | 'documento' | 'tipoDocumento' | 'tipoCliente' | 'esFrecuente' | 'telefono' | 'email'>>) => void
  aprobarRevision: (id: string) => void
  rechazarRevision: (id: string) => void
  findClienteByDoc: (doc: string) => Cliente | undefined
  pedidosHoyPorCliente: (clienteId: string) => Pedido[]
}

const Ctx = createContext<AppCtx | null>(null)

export function useApp() {
  const context = useContext(Ctx)
  if (!context) {
    throw new Error('useApp debe utilizarse dentro de AppProvider')
  }
  return context
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [view, setView] = useState<View>('dashboard')
  const [clientes, setClientes] = useState<Cliente[]>(mockClientes)
  const [pedidos, setPedidos] = useState<Pedido[]>(mockPedidos)

  const addCliente = (c: Cliente) => setClientes(prev => [...prev, c])

  const addPedido = (p: Pedido) => setPedidos(prev => [...prev, p])

  const updatePedidoEstado = (id: string, estado: Pedido['estado'], motivo?: Pedido['motivoFalla'], nota?: string) => {
    setPedidos(prev => prev.map(p =>
      p.id === id
        ? { ...p, estado, motivoFalla: motivo, notaFalla: nota, actualizadoEn: new Date().toISOString() }
        : p
    ))
  }

  const updatePedido = (id: string, fields: Partial<Pick<Pedido, 'clienteId' | 'descripcion' | 'peso' | 'monto' | 'kilometros' | 'fechaEntrega' | 'hojaRutaGenerada' | 'remitoGenerado' | 'recargoReprogramacion'>>) => {
    setPedidos(prev => prev.map(p =>
      p.id === id ? { ...p, ...fields, actualizadoEn: new Date().toISOString() } : p
    ))
  }

  const updateCliente = (id: string, fields: Partial<Pick<Cliente, 'nombre' | 'documento' | 'tipoDocumento' | 'tipoCliente' | 'esFrecuente' | 'telefono' | 'email'>>) => {
    setClientes(prev => prev.map(c => c.id === id ? { ...c, ...fields } : c))
  }

  const aprobarRevision = (id: string) => {
    setPedidos(prev => prev.map(p =>
      p.id === id ? { ...p, estado: 'pendiente', alertas: [], actualizadoEn: new Date().toISOString() } : p
    ))
  }

  const rechazarRevision = (id: string) => {
    setPedidos(prev => prev.map(p =>
      p.id === id ? { ...p, estado: 'fallido', motivoFalla: 'Dirección Inexistente', actualizadoEn: new Date().toISOString() } : p
    ))
  }

  const findClienteByDoc = (doc: string) =>
    clientes.find(c => c.documento.replace(/[-]/g, '') === doc.replace(/[-]/g, ''))

  const pedidosHoyPorCliente = (clienteId: string) => {
    const tod = new Date().toISOString().split('T')[0]
    return pedidos.filter(p => p.clienteId === clienteId && p.creadoEn.startsWith(tod))
  }

  return (
    <Ctx.Provider value={{ view, setView, clientes, pedidos, addCliente, addPedido, updatePedidoEstado, updatePedido, updateCliente, aprobarRevision, rechazarRevision, findClienteByDoc, pedidosHoyPorCliente }}>
      {children}
    </Ctx.Provider>
  )
}

export function generateId() {
  return Math.random().toString(36).slice(2, 9).toUpperCase()
}

export function parseAddress(raw: string): Partial<Direccion> & { sinNumero?: boolean } {
  const s = raw.trim()

  // Extract piso/depto: "Piso 3 Dpto B", "3° A", "PB", "PA", etc.
  let piso = '', depto = ''
  const pisoDptoMatch = s.match(/[Pp]iso\s*(\d+|PB|PA)[,\s]*(?:[Dd]pto?|[Dd]epartamento|[Aa]pto?)\.?\s*([A-Za-z0-9]+)?/i)
  if (pisoDptoMatch) { piso = pisoDptoMatch[1]; depto = pisoDptoMatch[2] || '' }
  else {
    const dptoOnly = s.match(/[Dd]pto?\.?\s*([A-Za-z0-9]+)/i)
    if (dptoOnly) depto = dptoOnly[1]
  }

  // Extract localidad: after last comma (ignoring piso/depto part)
  const parts = s.split(',')
  const localidadRaw = parts.length > 1 ? parts[parts.length - 1].trim() : ''
  const localidad = localidadRaw.replace(/\b(Argentina|AR)\b/gi, '').trim() || ''

  // Strip piso/depto and localidad from main part
  let main = parts[0].trim()

  // Extract street number (altura): last number in main part
  const alturaMatch = main.match(/\b(\d+)\s*$/) || main.match(/\b(\d+)\b/)
  const altura = alturaMatch ? alturaMatch[1] : '0'

  // Street = main stripped of the number at end
  const calle = main.replace(/\s*\b\d+\s*$/, '').trim() || main.split(/\s+/).slice(0, -1).join(' ')

  // Detect ambiguity
  const sinNumero = !alturaMatch || altura === '0' || /s\/n|sin\s+n[úu]mero/i.test(s)

  return { calle, altura, piso, depto, localidad, provincia: 'Buenos Aires', sinNumero }
}

export function detectAlertas(dir: Partial<Direccion> & { sinNumero?: boolean }): AlertaRevision[] {
  const alertas: AlertaRevision[] = []
  if (!dir.altura || dir.altura === '0' || (dir as any).sinNumero) alertas.push('altura_cero')
  if (!dir.calle || dir.calle.length < 3) alertas.push('geo_ambigua')
  return alertas
}
