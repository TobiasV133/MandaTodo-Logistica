import { useMemo, useState } from 'react'
import { useApp } from '../store'
import type { EstadoPedido, Pedido } from '../types'

const states: { value: EstadoPedido | 'todos'; label: string; tone: string }[] = [
  { value: 'todos', label: 'Todos', tone: 'bg-[#F1F5F9] text-[#334155]' },
  { value: 'pendiente', label: 'Pendiente', tone: 'bg-[#E0F2FE] text-[#0369A1]' },
  { value: 'en_revision', label: 'En revisión', tone: 'bg-[#FEF3C7] text-[#92400E]' },
  { value: 'en_transito', label: 'En tránsito', tone: 'bg-[#DBEAFE] text-[#1D4ED8]' },
  { value: 'cancelado_transito', label: 'Cancelado en tránsito', tone: 'bg-[#F1F5F9] text-[#475569]' },
  { value: 'entregado', label: 'Entregado', tone: 'bg-[#DCFCE7] text-[#15803D]' },
  { value: 'fallido', label: 'Fallido / Ausente', tone: 'bg-[#FEE2E2] text-[#B91C1C]' },
]

const dot: Record<EstadoPedido, string> = {
  pendiente: 'bg-[#0369A1]',
  en_revision: 'bg-[#92400E]',
  en_transito: 'bg-[#2563EB]',
  entregado: 'bg-[#15803D]',
  fallido: 'bg-[#B91C1C]',
  cancelado_transito: 'bg-[#2563EB]',
}

const field = 'w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2 text-sm text-[#0F172A] focus:outline-none focus:border-[#2563EB] disabled:opacity-40 disabled:cursor-not-allowed'

export default function GestionPedidos() {
  const { pedidos, clientes, updatePedido, updatePedidoEstado } = useApp()
  const [filter, setFilter] = useState<EstadoPedido | 'todos'>('todos')
  const [search, setSearch] = useState('')
  const [addressId, setAddressId] = useState<string | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [remito, setRemito] = useState<{ pedido: Pedido; duplicate: boolean; reason?: string } | null>(null)
  const [reprintId, setReprintId] = useState<string | null>(null)
  const [reprintReason, setReprintReason] = useState('Extravío')
  const [customReprintReason, setCustomReprintReason] = useState('')
  const [surchargeId, setSurchargeId] = useState<string | null>(null)
  const [surcharge, setSurcharge] = useState('50')
  const [notice, setNotice] = useState('')

  const visible = useMemo(() => pedidos
    .filter(p => filter === 'todos' || p.estado === filter)
    .filter(p => {
      const client = clientes.find(c => c.id === p.clienteId)
      const q = search.toLowerCase()
      return !q || `${p.id} ${p.descripcion} ${client?.nombre} ${client?.documento}`.toLowerCase().includes(q)
    })
    .sort((a, b) => b.actualizadoEn.localeCompare(a.actualizadoEn)), [pedidos, clientes, filter, search])

  const selected = pedidos.find(p => p.id === detailId)
  const reprintOrder = pedidos.find(p => p.id === reprintId)
  const surchargeOrder = pedidos.find(p => p.id === surchargeId)

  function clientFor(p: Pedido) { return clientes.find(c => c.id === p.clienteId) }
  function addressFor(p: Pedido) {
    const client = clientFor(p)
    return client?.direcciones.find(d => d.id === p.direccionId) ?? client?.direcciones[0]
  }
  function labelFor(state: EstadoPedido) { return states.find(s => s.value === state)?.label }
  function showNotice(text: string) {
    setNotice(text)
    window.setTimeout(() => setNotice(''), 2200)
  }
  function openRemito(p: Pedido) {
    updatePedido(p.id, { remitoGenerado: true })
    setRemito({ pedido: p, duplicate: false })
  }
  function confirmReprint() {
    if (!reprintOrder) return
    setRemito({ pedido: reprintOrder, duplicate: true, reason: reprintReason === 'Otro' ? customReprintReason || 'Otro motivo' : reprintReason })
    setReprintId(null)
  }
  function applySurcharge() {
    if (!surchargeOrder) return
    updatePedido(surchargeOrder.id, { recargoReprogramacion: surcharge })
    setSurchargeId(null)
    showNotice(`Recargo del ${surcharge}% aplicado`)
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <div className="px-6 pt-5 space-y-4 flex-shrink-0">
        <div>
          <h2 className="text-base font-semibold">Gestión de pedidos</h2>
          <p className="text-xs text-[#64748B] mt-1">{visible.length} pedidos en la vista actual</p>
        </div>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar por código, cliente, DNI/CUIT o descripción..." className={field} />
        <div className="flex bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg p-1 overflow-x-auto">
          {states.map(state => {
            const count = state.value === 'todos' ? pedidos.length : pedidos.filter(p => p.estado === state.value).length
            return <button key={state.value} onClick={() => setFilter(state.value)} className={`flex-1 min-w-max px-3 py-2 rounded text-xs transition-colors ${filter === state.value ? 'bg-[#2563EB] text-white' : 'text-[#64748B] hover:text-[#334155] hover:bg-[#F8FAFC]'}`}>{state.label} <span className="font-mono opacity-70 ml-1">{count}</span></button>
          })}
        </div>
      </div>

      <div className="flex-1 overflow-auto px-6 py-4">
        <div className="min-w-[940px] bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-[#F1F5F9] border-b border-[#E2E8F0]"><tr>{['ID', 'Cliente', 'Descripción', 'Peso / Monto', 'Estado', 'Entrega', 'Acciones'].map(h => <th key={h} className="px-4 py-3 text-[10px] font-mono uppercase tracking-wider text-[#64748B]">{h}</th>)}</tr></thead>
            <tbody className="divide-y divide-[#E2E8F0]">
              {visible.map(p => {
                const client = clientFor(p)
                return (
                  <tr key={p.id} className="hover:bg-[#F1F5F9] transition-colors">
                    <td className="px-4 py-4 font-mono text-xs text-[#2563EB]">{p.id}</td>
                    <td className="px-4 py-4"><div className="text-xs text-[#0F172A]">{client?.nombre}</div><div className="text-[10px] font-mono text-[#64748B] mt-0.5">{client?.tipoCliente}</div></td>
                    <td className="px-4 py-4 max-w-56"><div className="text-xs text-[#334155] truncate">{p.descripcion}</div>{p.hojaRutaGenerada && <span className="text-[9px] text-[#1D4ED8]">Hoja de ruta generada</span>}</td>
                    <td className="px-4 py-4"><div className="text-xs text-[#64748B]">{p.peso}</div><div className="font-mono text-xs">${p.monto}</div></td>
                    <td className="px-4 py-4"><span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-medium ${states.find(s => s.value === p.estado)?.tone}`}><span className={`mr-1.5 h-1.5 w-1.5 rounded-full ${dot[p.estado]}`} />{labelFor(p.estado)}</span>{p.recargoReprogramacion && <div className="text-[9px] text-[#92400E] mt-1">Recargo {p.recargoReprogramacion}%</div>}</td>
                    <td className="px-4 py-4 font-mono text-xs text-[#475569]">{p.fechaEntrega || '—'}</td>
                    <td className="px-4 py-4">
                      <div className="flex justify-end gap-1.5">
                        <button onClick={() => setAddressId(addressId === p.id ? null : p.id)} className="border border-[#CBD5E1] bg-[#F1F5F9] hover:bg-[#E2E8F0] text-[#334155] text-[10px] px-3 py-1.5 rounded">Dir.</button>
                        <button onClick={() => setDetailId(p.id)} className="border border-[#CBD5E1] bg-[#F1F5F9] hover:bg-[#E2E8F0] text-[#334155] text-[10px] px-3 py-1.5 rounded">Editar</button>
                      </div>
                      {addressId === p.id && <AddressInline order={p} address={addressFor(p)} />}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {visible.length === 0 && <div className="p-12 text-center text-xs text-[#64748B]">No hay pedidos que coincidan con los filtros.</div>}
        </div>
      </div>

      {selected && (
        <div className="fixed inset-0 z-[900] bg-black/70 backdrop-blur-sm flex justify-end">
          <div className="w-full max-w-xl bg-[#FFFFFF] border-l border-[#CBD5E1] h-full overflow-y-auto shadow-sm">
            <div className="sticky top-0 bg-[#FFFFFF]/95 backdrop-blur border-b border-[#E2E8F0] px-6 py-5 flex items-start justify-between z-10">
              <div><div className="font-mono text-xs text-[#2563EB]">{selected.id}</div><h3 className="text-lg font-semibold mt-1">Editar pedido</h3></div>
              <button onClick={() => setDetailId(null)} className="text-[#64748B] hover:text-[#0F172A]">Cerrar</button>
            </div>
            <div className="p-6 space-y-6">
              {selected.estado === 'cancelado_transito' && <div className="bg-[#2563EB]/10 border border-[#2563EB]/30 text-[#1D4ED8] rounded-lg p-3 text-xs"><strong>Pedido en pausa.</strong> La edición está bloqueada porque fue cancelado con una hoja de ruta activa.</div>}
              <section className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-5 space-y-4">
                <label><span className="block text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Cliente</span><select disabled={selected.estado === 'cancelado_transito'} value={selected.clienteId} onChange={e => updatePedido(selected.id, { clienteId: e.target.value })} className={field}>{clientes.map(client => <option key={client.id} value={client.id}>{client.nombre} · {client.documento}</option>)}</select></label>
                <div><div className="text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Descripción</div><input disabled={selected.estado === 'cancelado_transito'} value={selected.descripcion} onChange={e => updatePedido(selected.id, { descripcion: e.target.value })} className={field} /></div>
                <div className="grid grid-cols-3 gap-3">
                  <label><span className="block text-[10px] text-[#64748B] mb-1">Kgs</span><input disabled={selected.estado === 'cancelado_transito'} value={selected.peso} onChange={e => updatePedido(selected.id, { peso: e.target.value })} className={field} /></label>
                  <label><span className="block text-[10px] text-[#64748B] mb-1">Precio / Monto sugerido</span><input disabled={selected.estado === 'cancelado_transito'} value={selected.monto} onChange={e => updatePedido(selected.id, { monto: e.target.value })} className={field} /></label>
                  <label><span className="block text-[10px] text-[#64748B] mb-1">Kilómetros</span><input disabled={selected.estado === 'cancelado_transito'} value={selected.kilometros} onChange={e => updatePedido(selected.id, { kilometros: e.target.value })} className={field} /></label>
                </div>
                <label><span className="block text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Estado</span><select value={selected.estado} onChange={e => updatePedidoEstado(selected.id, e.target.value as EstadoPedido)} className={field}>{states.filter(state => state.value !== 'todos').map(state => <option key={state.value} value={state.value}>{state.label}</option>)}</select></label>
                <div className="flex justify-end"><button onClick={() => setDetailId(null)} className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs px-5 py-2.5 rounded-lg">Guardar cambios</button></div>
              </section>

              <section>
                <h4 className="text-xs font-mono uppercase tracking-wider text-[#64748B] mb-3">Acciones operativas</h4>
                <div className="grid grid-cols-2 gap-2">
                  {selected.remitoGenerado && <button onClick={() => setReprintId(selected.id)} className="border border-[#2563EB]/40 text-[#1D4ED8] hover:bg-[#2563EB]/10 text-xs py-2.5 rounded-lg">Reimprimir Remito</button>}
                  {!selected.remitoGenerado && selected.estado === 'pendiente' && <button onClick={() => openRemito(selected)} className="bg-[#2563EB] text-white text-xs py-2.5 rounded-lg">Generar Remito</button>}
                  {selected.hojaRutaGenerada && selected.estado !== 'cancelado_transito' && <button onClick={() => updatePedidoEstado(selected.id, 'cancelado_transito')} className="border border-[#2563EB]/40 text-[#1D4ED8] hover:bg-[#2563EB]/10 text-xs py-2.5 rounded-lg">Cancelar en tránsito / Pausar</button>}
                  {selected.estado === 'fallido' && <button onClick={() => { setSurchargeId(selected.id); setSurcharge(selected.recargoReprogramacion ?? '50') }} className="border border-[#92400E]/30 text-[#92400E] hover:bg-[#FEF3C7] text-xs py-2.5 rounded-lg">Aplicar recargo</button>}
                </div>
              </section>

            </div>
          </div>
        </div>
      )}

      {reprintOrder && (
        <Modal title="Motivo de reimpresión" subtitle={`Remito del pedido ${reprintOrder.id}`} onClose={() => setReprintId(null)}>
          <div className="space-y-3">{['Extravío', 'Deterioro', 'Solicitud de cliente', 'Otro'].map(reason => <button key={reason} onClick={() => setReprintReason(reason)} className={`w-full text-left border rounded-lg px-4 py-3 text-sm ${reprintReason === reason ? 'border-[#2563EB] bg-[#2563EB]/10 text-[#1D4ED8]' : 'border-[#E2E8F0] text-[#475569]'}`}>{reason}</button>)}</div>
          {reprintReason === 'Otro' && <input value={customReprintReason} onChange={e => setCustomReprintReason(e.target.value)} placeholder="Ingresá el motivo de reimpresión..." className={`${field} mt-3`} />}
          <button onClick={confirmReprint} className="w-full mt-5 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm py-2.5 rounded-lg">Continuar a vista previa</button>
        </Modal>
      )}

      {surchargeOrder && (
        <Modal title="Recargo por reprogramación" subtitle={`Pedido ${surchargeOrder.id} · ${surchargeOrder.motivoFalla ?? 'Ausente'}`} onClose={() => setSurchargeId(null)}>
          <div className="bg-[#FEF3C7] border border-[#92400E]/20 text-[#92400E] text-xs rounded-lg p-3 mb-4">Sugerencia operativa: aplicar un recargo del 50% por visita fallida. Podés editar el porcentaje.</div>
          <label><span className="block text-xs text-[#64748B] mb-1.5">Recargo</span><div className="relative"><input type="number" value={surcharge} onChange={e => setSurcharge(e.target.value)} className={field} /><span className="absolute right-3 top-2 text-sm text-[#64748B]">%</span></div></label>
          <button onClick={applySurcharge} className="w-full mt-5 bg-[#2563EB] text-white text-sm py-2.5 rounded-lg">Aplicar recargo</button>
        </Modal>
      )}

      {remito && <RemitoPreview order={remito.pedido} duplicate={remito.duplicate} reason={remito.reason} client={clientFor(remito.pedido)} address={addressFor(remito.pedido)} onClose={() => setRemito(null)} onAction={showNotice} />}
      {notice && <div className="fixed bottom-5 right-5 z-[1200] bg-[#FFFFFF] border border-[#BFDBFE] text-[#0F172A] text-xs px-4 py-3 rounded-lg shadow-sm">{notice}</div>}
    </div>
  )
}

function AddressInline({ order, address }: { order: Pedido; address?: ReturnType<typeof useAddressShape> }) {
  return <div className="absolute right-6 mt-2 z-20 w-80 bg-[#FFFFFF] border border-[#CBD5E1] rounded-lg p-3 shadow-sm text-left"><div className="text-[9px] font-mono uppercase text-[#64748B] mb-1">Dirección de {order.id}</div><div className="text-xs text-[#0F172A]">{address ? `${address.calle} ${address.altura}${address.piso ? `, Piso ${address.piso}` : ''}${address.depto ? ` Dpto ${address.depto}` : ''}` : 'Sin dirección'}</div><div className="text-[10px] text-[#64748B] mt-1">{address?.localidad}, {address?.provincia}</div></div>
}

function useAddressShape() {
  return { calle: '', altura: '', piso: '', depto: '', localidad: '', provincia: '' }
}

function Modal({ title, subtitle, onClose, children }: { title: string; subtitle: string; onClose: () => void; children: React.ReactNode }) {
  return <div className="fixed inset-0 z-[1000] bg-black/45 flex items-center justify-center p-4"><div className="w-full max-w-md bg-[#FFFFFF] border border-[#CBD5E1] rounded-xl shadow-sm"><div className="px-6 py-5 border-b border-[#E2E8F0] flex justify-between"><div><h3 className="text-base font-semibold">{title}</h3><p className="text-xs text-[#64748B] mt-1">{subtitle}</p></div><button onClick={onClose} className="text-[#64748B] hover:text-[#0F172A]">Cerrar</button></div><div className="p-6">{children}</div></div></div>
}

function RemitoPreview({ order, duplicate, reason, client, address, onClose, onAction }: { order: Pedido; duplicate: boolean; reason?: string; client: any; address: any; onClose: () => void; onAction: (s: string) => void }) {
  return (
    <div className="fixed inset-0 z-[1100] bg-[#0F172A]/95 overflow-y-auto p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-4"><div><h3 className="text-base font-semibold">Vista previa de remito</h3><p className="text-xs text-[#64748B]">Formato A4 · Pedido {order.id}</p></div><button onClick={onClose} className="text-xs text-[#475569] border border-[#CBD5E1] px-3 py-2 rounded">Cerrar vista previa</button></div>
        <div className="bg-white text-slate-900 mx-auto shadow-sm min-h-[900px] max-w-[690px] p-12 relative">
          {duplicate && <div className="absolute inset-x-0 top-0 bg-[#2563EB] text-white text-center font-bold tracking-[0.22em] py-3">DUPLICADO / REIMPRESIÓN</div>}
          <div className={duplicate ? 'pt-8' : ''}>
            <div className="flex justify-between items-start border-b-2 border-slate-900 pb-5"><div><div className="text-2xl font-bold">MandáTodo</div><div className="text-xs text-slate-500 mt-1">Logística y distribución</div></div><div className="text-right"><div className="text-xs text-slate-500 uppercase">Remito</div><div className="text-xl font-mono font-bold">{order.id}</div><div className="text-xs text-slate-500">{order.creadoEn}</div></div></div>
            {duplicate && <div className="mt-5 border border-violet-300 bg-violet-50 text-violet-800 rounded p-3 text-xs"><strong>Copia reimpresa.</strong> Motivo: {reason} · Emitida por Administrador</div>}
            <div className="grid grid-cols-2 gap-8 py-7 border-b border-slate-200"><div><div className="text-[10px] uppercase font-bold text-slate-400 mb-2">Cliente</div><div className="font-semibold">{client?.nombre}</div><div className="text-sm text-slate-600">{client?.tipoDocumento} {client?.documento}</div><div className="text-sm text-slate-600">{client?.email}</div></div><div><div className="text-[10px] uppercase font-bold text-slate-400 mb-2">Dirección de entrega</div><div className="font-semibold">{address?.calle} {address?.altura}</div><div className="text-sm text-slate-600">{address?.piso ? `Piso ${address.piso} ` : ''}{address?.depto ? `Dpto ${address.depto}` : ''}</div><div className="text-sm text-slate-600">{address?.localidad}, {address?.provincia}</div></div></div>
            <table className="w-full mt-8"><thead><tr className="border-b-2 border-slate-800"><th className="text-left text-xs uppercase py-3">Descripción</th><th className="text-right text-xs uppercase py-3">Peso</th><th className="text-right text-xs uppercase py-3">Kilómetros</th><th className="text-right text-xs uppercase py-3">Costo</th></tr></thead><tbody><tr className="border-b border-slate-200"><td className="py-5 text-sm">{order.descripcion}</td><td className="py-5 text-sm text-right">{order.peso}</td><td className="py-5 text-sm text-right">{order.kilometros} km</td><td className="py-5 text-sm text-right font-semibold">${order.monto}</td></tr></tbody></table>
            <div className="mt-32 grid grid-cols-2 gap-16"><div className="border-t border-slate-500 pt-2 text-center text-xs text-slate-500">Firma del receptor</div><div className="border-t border-slate-500 pt-2 text-center text-xs text-slate-500">Aclaración y DNI</div></div>
            <div className="absolute bottom-8 inset-x-12 text-center text-[10px] text-slate-400">Documento no válido como factura · MandáTodo · Operaciones internas</div>
          </div>
        </div>
        <div className="sticky bottom-4 mt-4 flex justify-center gap-3"><button onClick={() => onAction('Descarga de PDF simulada')} className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm px-6 py-3 rounded-md shadow-sm">Descargar PDF</button><button onClick={() => onAction('Documento enviado a impresión')} className="bg-[#F1F5F9] border border-[#CBD5E1] text-[#334155] text-sm px-6 py-3 rounded-md shadow-sm">Imprimir</button></div>
      </div>
    </div>
  )
}
