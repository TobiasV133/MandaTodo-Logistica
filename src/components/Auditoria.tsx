import { useState } from 'react'

const logs = [
  { fecha: '10/10/2025 14:32', usuario: 'Juan Nuñez', accion: 'modificó la dirección', objeto: 'Pedido #P1024', tipo: 'Edición' },
  { fecha: '10/10/2025 13:48', usuario: 'Sofia Gomez', accion: 'reimprimió un remito por Extravío', objeto: 'Pedido #P1019', tipo: 'Remito' },
  { fecha: '10/10/2025 12:15', usuario: 'Tobías Varela', accion: 'creó el pedido', objeto: 'Pedido #P1028', tipo: 'Alta' },
  { fecha: '10/10/2025 11:44', usuario: 'Tobías Varela', accion: 'validó la dirección y asignó hoja de ruta', objeto: 'Pedido #P1028', tipo: 'Edición' },
  { fecha: '10/10/2025 11:02', usuario: 'Juan Nuñez', accion: 'cambió el estado a Cancelado en Tránsito', objeto: 'Pedido #P1007', tipo: 'Estado' },
  { fecha: '09/10/2025 18:21', usuario: 'Administrador', accion: 'activó al usuario Tobías Varela', objeto: 'Usuarios', tipo: 'Seguridad' },
  { fecha: '09/10/2025 16:08', usuario: 'Sofia Gomez', accion: 'cambió el estado de Pendiente a En Tránsito', objeto: 'Pedido #P1019', tipo: 'Estado' },
]

export default function Auditoria() {
  const [operario, setOperario] = useState('Todos')
  const [fecha, setFecha] = useState('')
  const [search, setSearch] = useState('')
  const selectedDate = fecha ? fecha.split('-').reverse().join('/') : ''
  const visible = logs.filter(log => {
    const matchesOperator = operario === 'Todos' || log.usuario === operario
    const matchesDate = !selectedDate || log.fecha.startsWith(selectedDate)
    const query = search.trim().toLowerCase()
    const matchesSearch = !query || `${log.fecha} ${log.usuario} ${log.objeto} ${log.accion}`.toLowerCase().includes(query)
    return matchesOperator && matchesDate && matchesSearch
  })
  const field = 'bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#2563EB]'
  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="max-w-5xl mx-auto space-y-5">
        <div><h2 className="text-base font-semibold">Auditoría global</h2><p className="text-xs text-[#64748B] mt-1">Registro cronológico e inalterable de la actividad operativa.</p></div>
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-4 grid grid-cols-1 md:grid-cols-[1fr_auto_auto_auto] items-end gap-3">
          <label><span className="block text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Búsqueda general</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Pedido #P1024, operario o fecha..." className={`${field} w-full`} /></label>
          <label><span className="block text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Fecha exacta</span><input type="date" value={fecha} onChange={e => setFecha(e.target.value)} className={field} /></label>
          <label><span className="block text-[10px] font-mono uppercase text-[#64748B] mb-1.5">Operario</span><select value={operario} onChange={e => setOperario(e.target.value)} className={`${field} min-w-48`}><option>Todos</option><option>Juan Nuñez</option><option>Sofia Gomez</option><option>Tobías Varela</option><option>Administrador</option></select></label>
          <button onClick={() => { setSearch(''); setFecha(''); setOperario('Todos') }} className="text-xs text-[#2563EB] px-3 py-2">Limpiar filtros</button>
        </div>
        <div className="text-right text-xs font-mono text-[#64748B]">{visible.length} eventos encontrados</div>
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-[#F1F5F9] border-b border-[#E2E8F0]"><tr>{['Fecha y hora', 'Operario', 'Actividad', 'Tipo'].map(h => <th key={h} className="px-5 py-3 text-[10px] font-mono uppercase tracking-wider text-[#64748B]">{h}</th>)}</tr></thead>
            <tbody className="divide-y divide-[#E2E8F0]">{visible.map((log, i) => <tr key={i} className="hover:bg-[#F1F5F9]"><td className="px-5 py-4 font-mono text-xs text-[#64748B]">{log.fecha}</td><td className="px-5 py-4 text-xs text-[#0F172A]">{log.usuario}</td><td className="px-5 py-4 text-xs text-[#475569]">{log.accion} <span className="text-[#1D4ED8]">{log.objeto}</span></td><td className="px-5 py-4"><span className="text-[10px] font-mono bg-[#2563EB]/10 text-[#2563EB] border border-[#2563EB]/20 px-2 py-1 rounded">{log.tipo}</span></td></tr>)}</tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
