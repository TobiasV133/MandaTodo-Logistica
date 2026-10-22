import { useState } from 'react'
import { useApp } from '../store'
import type { TipoCliente, TipoDocumento } from '../types'

const inCls = 'w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2 text-sm text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors disabled:opacity-50'

export default function Clientes() {
  const { clientes, pedidos, updateCliente } = useApp()
  const [search, setSearch] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)

  // edit form state
  const [eNombre, setENombre] = useState('')
  const [eDoc, setEDoc] = useState('')
  const [eTipoDoc, setETipoDoc] = useState<TipoDocumento>('DNI')
  const [eTipoCliente, setETipoCliente] = useState<TipoCliente>('Particular')
  const [eFrecuente, setEFrecuente] = useState(false)
  const [eTelefono, setETelefono] = useState('')
  const [eEmail, setEEmail] = useState('')

  const filtered = clientes.filter(c => {
    const q = search.toLowerCase()
    return (
      c.nombre.toLowerCase().includes(q) ||
      c.documento.replace(/-/g, '').includes(q.replace(/-/g, '')) ||
      c.email.toLowerCase().includes(q)
    )
  })

  function openEdit(id: string) {
    const c = clientes.find(x => x.id === id)!
    setEditingId(id)
    setENombre(c.nombre)
    setEDoc(c.documento)
    setETipoDoc(c.tipoDocumento)
    setETipoCliente(c.tipoCliente)
    setEFrecuente(c.esFrecuente)
    setETelefono(c.telefono)
    setEEmail(c.email)
  }

  function save(id: string) {
    updateCliente(id, {
      nombre: eNombre, documento: eDoc, tipoDocumento: eTipoDoc,
      tipoCliente: eTipoCliente, esFrecuente: eFrecuente,
      telefono: eTelefono, email: eEmail,
    })
    setEditingId(null)
  }

  function pedidosDeCliente(clienteId: string) {
    return pedidos.filter(p => p.clienteId === clienteId)
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">

      {/* Header */}
      <div className="px-6 pt-5 pb-0 flex-shrink-0 space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-[#0F172A]">Clientes</h2>
            <p className="text-xs text-[#64748B] mt-0.5">{filtered.length} cliente{filtered.length !== 1 ? 's' : ''} registrado{filtered.length !== 1 ? 's' : ''}</p>
          </div>
        </div>
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Buscar por nombre, DNI/CUIT o email..."
          className="w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2 text-sm text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors"
        />
      </div>

      {/* List */}
      <div className="flex-1 overflow-auto px-6 py-4">
        {filtered.length === 0 ? (
          <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-12 text-center text-xs text-[#64748B]">
            {search ? 'Sin resultados.' : 'No hay clientes registrados.'}
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map(c => {
              const isEditing = editingId === c.id
              const ps = pedidosDeCliente(c.id)
              const activos = ps.filter(p => p.estado === 'pendiente' || p.estado === 'en_revision').length

              return (
                <div key={c.id} className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">
                  {/* Row */}
                  <div className="flex items-center gap-4 px-5 py-4">
                    <div className="w-9 h-9 rounded-full bg-[#BFDBFE] flex items-center justify-center text-sm font-semibold text-[#2563EB] flex-shrink-0">
                      {c.nombre.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-medium text-[#0F172A]">{c.nombre}</span>
                        <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded border ${c.tipoCliente === 'Empresa' ? 'bg-[#E2E8F0] text-[#2563EB] border-[#2563EB]/20' : 'bg-[#E2E8F0] text-[#0369A1] border-[#0369A1]/20'}`}>
                          {c.tipoCliente}
                        </span>
                        {c.esFrecuente && <span className="text-[9px] text-[#92400E]">★ Frecuente</span>}
                      </div>
                      <div className="flex items-center gap-3 mt-0.5 flex-wrap">
                        <span className="font-mono text-xs text-[#0369A1]">{c.tipoDocumento} {c.documento}</span>
                        {c.telefono && <span className="text-xs text-[#64748B]">{c.telefono}</span>}
                        {c.email && <span className="text-xs text-[#64748B]">{c.email}</span>}
                      </div>
                    </div>
                    <div className="flex items-center gap-4 flex-shrink-0">
                      <div className="text-right">
                        <div className="font-mono text-sm text-[#0F172A]">{ps.length}</div>
                        <div className="text-[10px] text-[#64748B]">pedidos</div>
                      </div>
                      {activos > 0 && (
                        <div className="text-right">
                          <div className="font-mono text-sm text-[#0369A1]">{activos}</div>
                          <div className="text-[10px] text-[#64748B]">activos</div>
                        </div>
                      )}
                      <button
                        onClick={() => isEditing ? setEditingId(null) : openEdit(c.id)}
                        className={`text-xs px-3 py-1.5 rounded border transition-colors ${
                          isEditing
                            ? 'border-[#E2E8F0] text-[#64748B]'
                            : 'border-[#2563EB]/40 text-[#2563EB] hover:bg-[#2563EB]/10'
                        }`}
                      >
                        {isEditing ? 'Cerrar' : 'Editar'}
                      </button>
                    </div>
                  </div>

                  {/* Inline edit form */}
                  {isEditing && (
                    <div className="border-t border-[#E2E8F0] bg-[#F8FAFC] px-5 py-5 space-y-4">
                      <div className="text-[10px] font-mono text-[#64748B] uppercase tracking-wider mb-1">Editar datos del cliente</div>

                      <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
                        <div className="lg:col-span-2">
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">Nombre / Razón social</label>
                          <input value={eNombre} onChange={e => setENombre(e.target.value)} className={inCls} />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">Tipo de documento</label>
                          <select value={eTipoDoc} onChange={e => setETipoDoc(e.target.value as TipoDocumento)}
                            className={inCls}>
                            <option value="DNI">DNI</option>
                            <option value="CUIT">CUIT</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">
                            Número de {eTipoDoc}
                          </label>
                          <input value={eDoc} onChange={e => setEDoc(e.target.value)} className={inCls} />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">Teléfono</label>
                          <input value={eTelefono} onChange={e => setETelefono(e.target.value)} className={inCls} />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">Email</label>
                          <input value={eEmail} onChange={e => setEEmail(e.target.value)} className={inCls} />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono text-[#64748B] uppercase mb-1.5">Tipo de cliente</label>
                          <select value={eTipoCliente} onChange={e => setETipoCliente(e.target.value as TipoCliente)}
                            className={inCls}>
                            <option value="Particular">Particular</option>
                            <option value="Empresa">Empresa</option>
                          </select>
                        </div>
                        <div className="flex items-end pb-1">
                          <label className="flex items-center gap-2.5 cursor-pointer">
                            <div
                              onClick={() => setEFrecuente(v => !v)}
                              className={`w-9 h-5 rounded-full transition-colors flex items-center cursor-pointer ${eFrecuente ? 'bg-[#2563EB]' : 'bg-[#E2E8F0]'}`}
                            >
                              <div className={`w-4 h-4 rounded-full bg-white shadow transition-transform mx-0.5 ${eFrecuente ? 'translate-x-4' : 'translate-x-0'}`} />
                            </div>
                            <span className="text-xs text-[#334155]">Cliente frecuente</span>
                          </label>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 pt-1">
                        <button
                          onClick={() => save(c.id)}
                          disabled={!eNombre || !eDoc}
                          className="bg-[#2563EB] disabled:opacity-40 hover:bg-[#1D4ED8] text-white text-xs px-5 py-2 rounded-lg transition-colors"
                        >
                          Guardar cambios
                        </button>
                        <button
                          onClick={() => setEditingId(null)}
                          className="text-xs text-[#64748B] hover:text-[#0F172A] transition-colors"
                        >
                          Cancelar
                        </button>
                        <span className="text-[10px] font-mono text-[#64748B] ml-auto">Alta: {c.creadoEn}</span>
                      </div>
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
