import { useState } from 'react'
import { useApp } from '../store'
import type { Cliente } from '../types'

export default function Libreta() {
  const { clientes, pedidos } = useApp()
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<Cliente | null>(null)
  const [filterTipo, setFilterTipo] = useState<'Todos' | 'Particular' | 'Empresa'>('Todos')

  const filtered = clientes.filter(c => {
    const matchSearch =
      c.nombre.toLowerCase().includes(search.toLowerCase()) ||
      c.documento.includes(search)
    const matchTipo = filterTipo === 'Todos' || c.tipoCliente === filterTipo
    return matchSearch && matchTipo
  })

  return (
    <div className="flex-1 flex overflow-hidden">

      {/* List */}
      <div className="w-80 flex-shrink-0 flex flex-col border-r border-[#1e2740]">
        <div className="p-4 border-b border-[#1e2740] space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-[#e8eaf0]">Libreta de clientes</h2>
            <span className="font-mono text-xs text-[#6b7694]">{filtered.length} reg.</span>
          </div>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Buscar por nombre o doc..."
            className="w-full bg-[#0a0d14] border border-[#1e2740] rounded-lg px-3 py-2 text-xs text-[#e8eaf0] placeholder-[#6b7694] focus:outline-none focus:border-[#8B5CF6] transition-colors"
          />
          <div className="flex gap-1">
            {(['Todos', 'Particular', 'Empresa'] as const).map(t => (
              <button
                key={t}
                onClick={() => setFilterTipo(t)}
                className={`flex-1 text-xs py-1.5 rounded transition-colors ${filterTipo === t ? 'bg-[#8B5CF6] text-white' : 'text-[#6b7694] hover:text-[#e8eaf0] bg-[#161c2d]'}`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto divide-y divide-[#1e2740]">
          {filtered.map(c => {
            const cPedidos = pedidos.filter(p => p.clienteId === c.id)
            const active = cPedidos.filter(p => p.estado === 'pendiente' || p.estado === 'en_revision').length
            return (
              <button
                key={c.id}
                onClick={() => setSelected(c)}
                className={`w-full text-left px-4 py-3.5 hover:bg-[#161c2d] transition-colors ${selected?.id === c.id ? 'bg-[#161c2d] border-l-2 border-[#8B5CF6]' : ''}`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <div className="text-xs font-medium text-[#e8eaf0] truncate">{c.nombre}</div>
                    <div className="font-mono text-[10px] text-[#6b7694] mt-0.5">{c.tipoDocumento} {c.documento}</div>
                  </div>
                  <div className="flex flex-col items-end gap-1 flex-shrink-0">
                    <span className={`text-[9px] px-1.5 py-0.5 rounded font-mono ${c.tipoCliente === 'Empresa' ? 'bg-[#1e2740] text-[#a78bfa]' : 'bg-[#1e2740] text-[#38bdf8]'}`}>
                      {c.tipoCliente}
                    </span>
                    {c.esFrecuente && <span className="text-[9px] text-[#f5a623]">★ Frecuente</span>}
                  </div>
                </div>
                {active > 0 && (
                  <div className="text-[10px] text-[#22d07a] mt-1">{active} pedido{active > 1 ? 's' : ''} activo{active > 1 ? 's' : ''}</div>
                )}
              </button>
            )
          })}
          {filtered.length === 0 && (
            <div className="p-6 text-center text-xs text-[#6b7694]">Sin resultados</div>
          )}
        </div>
      </div>

      {/* Detail */}
      <div className="flex-1 overflow-y-auto p-6">
        {!selected ? (
          <div className="flex items-center justify-center h-full text-sm text-[#6b7694]">
            Seleccioná un cliente para ver el detalle
          </div>
        ) : (
          <ClienteDetail cliente={selected} pedidosList={pedidos.filter(p => p.clienteId === selected.id)} />
        )}
      </div>
    </div>
  )
}

function ClienteDetail({ cliente, pedidosList }: { cliente: Cliente; pedidosList: ReturnType<typeof useApp>['pedidos'] }) {
  const estadoColors: Record<string, string> = {
    en_revision: 'text-[#f5a623]',
    pendiente: 'text-[#38bdf8]',
    entregado: 'text-[#22d07a]',
    fallido: 'text-[#ff4560]',
  }
  const estadoLabels: Record<string, string> = {
    en_revision: 'En revisión',
    pendiente: 'Pendiente',
    entregado: 'Entregado',
    fallido: 'Fallido',
  }

  return (
    <div className="max-w-2xl space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h3 className="text-base font-semibold text-[#e8eaf0]">{cliente.nombre}</h3>
            {cliente.esFrecuente && (
              <span className="text-[10px] font-mono bg-[#2a1f00] text-[#f5a623] px-2 py-0.5 rounded">★ Frecuente</span>
            )}
          </div>
          <div className="font-mono text-xs text-[#6b7694]">{cliente.tipoDocumento} {cliente.documento}</div>
        </div>
        <div className="flex flex-col gap-1 items-end">
          <span className={`text-xs px-2.5 py-1 rounded font-mono ${cliente.tipoCliente === 'Empresa' ? 'bg-[#1e2740] text-[#a78bfa]' : 'bg-[#1e2740] text-[#38bdf8]'}`}>
            {cliente.tipoCliente}
          </span>
          <span className="text-[10px] text-[#6b7694] font-mono">Alta: {cliente.creadoEn}</span>
        </div>
      </div>

      {/* Info grid */}
      <div className="bg-[#111520] border border-[#1e2740] rounded-lg p-5 grid grid-cols-2 gap-4">
        <InfoItem label="Teléfono" value={cliente.telefono} />
        <InfoItem label="Email" value={cliente.email} />
        <InfoItem label="Tipo de cliente" value={cliente.tipoCliente} />
        <InfoItem label="Carga masiva (Excel)" value={cliente.esFrecuente ? 'Habilitada' : 'No habilitada'} highlight={cliente.esFrecuente} />
      </div>

      {/* Addresses */}
      <div>
        <div className="text-xs font-mono text-[#6b7694] uppercase tracking-wider mb-3">Direcciones registradas</div>
        {cliente.direcciones.map(d => (
          <div key={d.id} className="bg-[#111520] border border-[#1e2740] rounded-lg p-4 space-y-1">
            <div className="text-sm text-[#e8eaf0]">
              {d.calle} {d.altura}{d.piso ? `, Piso ${d.piso}` : ''}{d.depto ? ` Dpto ${d.depto}` : ''}
            </div>
            <div className="text-xs text-[#6b7694]">{d.localidad}{d.provincia !== d.localidad ? `, ${d.provincia}` : ''}</div>
            {d.lat && d.lng && (
              <div className="font-mono text-[10px] text-[#6b7694]">{d.lat.toFixed(5)}, {d.lng.toFixed(5)}</div>
            )}
            <div className={`text-[10px] font-mono mt-1 ${d.confirmada ? 'text-[#22d07a]' : 'text-[#6b7694]'}`}>
              {d.confirmada ? '✓ Pin confirmado' : '◌ Pin sin confirmar'}
            </div>
          </div>
        ))}
        {cliente.direcciones.length === 0 && <div className="text-xs text-[#6b7694]">Sin direcciones registradas.</div>}
      </div>

      {/* Pedidos */}
      <div>
        <div className="text-xs font-mono text-[#6b7694] uppercase tracking-wider mb-3">
          Historial de pedidos ({pedidosList.length})
        </div>
        {pedidosList.length === 0 ? (
          <div className="text-xs text-[#6b7694]">Sin pedidos.</div>
        ) : (
          <div className="space-y-2">
            {pedidosList.map(p => (
              <div key={p.id} className="bg-[#111520] border border-[#1e2740] rounded-lg px-4 py-3 flex items-center justify-between gap-4">
                <div>
                  <div className="font-mono text-xs text-[#38bdf8]">{p.id}</div>
                  <div className="text-xs text-[#c0c8e0] mt-0.5">{p.descripcion}</div>
                  {p.motivoFalla && <div className="text-[10px] text-[#ff4560] mt-0.5">Motivo: {p.motivoFalla}</div>}
                </div>
                <div className="text-right flex-shrink-0">
                  <div className={`text-xs font-medium ${estadoColors[p.estado]}`}>{estadoLabels[p.estado]}</div>
                  <div className="font-mono text-[10px] text-[#6b7694] mt-0.5">{p.creadoEn}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function InfoItem({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div>
      <div className="text-[10px] text-[#6b7694] font-mono uppercase tracking-wider mb-0.5">{label}</div>
      <div className={`text-xs ${highlight ? 'text-[#22d07a]' : 'text-[#c0c8e0]'}`}>{value}</div>
    </div>
  )
}
