import { useState } from 'react'

type Rol = 'Administrador' | 'Operario'
type Usuario = { id: number; nombre: string; email: string; rol: Rol; activo: boolean }

const initialUsers: Usuario[] = [
  { id: 1, nombre: 'Juan Nuñez', email: 'juan.nunez@mandatodo.com', rol: 'Operario', activo: true },
  { id: 2, nombre: 'Sofia Gomez', email: 'sofia.gomez@mandatodo.com', rol: 'Administrador', activo: true },
  { id: 3, nombre: 'Tobías Varela', email: 'tobias.varela@mandatodo.com', rol: 'Operario', activo: true },
  { id: 4, nombre: 'Marina Costa', email: 'marina.costa@mandatodo.com', rol: 'Operario', activo: false },
]

const fieldCls = 'w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2.5 text-sm text-[#0F172A] focus:outline-none focus:border-[#2563EB]'

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState(initialUsers)
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Usuario | null>(null)
  const [draft, setDraft] = useState<Omit<Usuario, 'id'>>({ nombre: '', email: '', rol: 'Operario', activo: true })

  const visible = usuarios.filter(u => `${u.nombre} ${u.email} ${u.rol}`.toLowerCase().includes(search.toLowerCase()))

  function open(user?: Usuario) {
    setEditing(user ?? { id: 0, nombre: '', email: '', rol: 'Operario', activo: true })
    setDraft(user ? { nombre: user.nombre, email: user.email, rol: user.rol, activo: user.activo } : { nombre: '', email: '', rol: 'Operario', activo: true })
  }

  function save() {
    if (!editing || !draft.nombre || !draft.email) return
    if (editing.id) setUsuarios(prev => prev.map(u => u.id === editing.id ? { ...u, ...draft } : u))
    else setUsuarios(prev => [...prev, { id: Date.now(), ...draft }])
    setEditing(null)
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="max-w-5xl mx-auto space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-[#0F172A]">Gestión de usuarios</h2>
            <p className="text-xs text-[#64748B] mt-1">Administrá accesos, roles y estado de los operarios.</p>
          </div>
          <button onClick={() => open()} className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs font-medium px-4 py-2.5 rounded-lg transition-colors">+ Nuevo usuario</button>
        </div>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar por nombre, email o rol..." className={fieldCls} />
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-[#F1F5F9] border-b border-[#E2E8F0]">
              <tr>{['Operario', 'Email', 'Rol', 'Estado', ''].map(h => <th key={h} className="px-5 py-3 text-[10px] font-mono uppercase tracking-wider text-[#64748B]">{h}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-[#E2E8F0]">
              {visible.map(u => (
                <tr key={u.id} className="hover:bg-[#F1F5F9] transition-colors">
                  <td className="px-5 py-4"><div className="flex items-center gap-3"><span className="w-8 h-8 rounded-full bg-[#2563EB]/15 text-[#2563EB] flex items-center justify-center text-xs font-semibold">{u.nombre.split(' ').map(n => n[0]).join('').slice(0, 2)}</span><span className="text-sm text-[#0F172A]">{u.nombre}</span></div></td>
                  <td className="px-5 py-4 text-xs text-[#475569]">{u.email}</td>
                  <td className="px-5 py-4"><span className="text-[10px] font-mono bg-[#E2E8F0] text-[#1D4ED8] px-2 py-1 rounded">{u.rol}</span></td>
                  <td className="px-5 py-4"><span className={`text-[10px] font-medium ${u.activo ? 'text-[#15803D]' : 'text-[#64748B]'}`}>{u.activo ? '● Activo' : '● Inactivo'}</span></td>
                  <td className="px-5 py-4 text-right"><button onClick={() => open(u)} className="text-xs border border-[#2563EB]/40 text-[#2563EB] hover:bg-[#2563EB]/10 px-3 py-1.5 rounded">Editar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {editing && (
        <div className="fixed inset-0 z-[1000] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-[#FFFFFF] border border-[#CBD5E1] rounded-xl shadow-sm">
            <div className="px-6 py-5 border-b border-[#E2E8F0] flex justify-between">
              <div><h3 className="text-base font-semibold">{editing.id ? 'Editar usuario' : 'Nuevo usuario'}</h3><p className="text-xs text-[#64748B] mt-1">Datos de acceso y permisos</p></div>
              <button onClick={() => setEditing(null)} className="text-[#64748B] hover:text-[#0F172A]">✕</button>
            </div>
            <div className="p-6 space-y-4">
              <label className="block"><span className="block text-xs text-[#64748B] mb-1.5">Nombre</span><input value={draft.nombre} onChange={e => setDraft({ ...draft, nombre: e.target.value })} className={fieldCls} /></label>
              <label className="block"><span className="block text-xs text-[#64748B] mb-1.5">Email</span><input type="email" value={draft.email} onChange={e => setDraft({ ...draft, email: e.target.value })} className={fieldCls} /></label>
              <label className="block"><span className="block text-xs text-[#64748B] mb-1.5">Rol</span><select value={draft.rol} onChange={e => setDraft({ ...draft, rol: e.target.value as Rol })} className={fieldCls}><option>Operario</option><option>Administrador</option></select></label>
              <button onClick={() => setDraft({ ...draft, activo: !draft.activo })} className="w-full flex items-center justify-between border border-[#E2E8F0] rounded-lg px-3 py-3 text-sm"><span>Estado del usuario</span><span className={draft.activo ? 'text-[#15803D]' : 'text-[#64748B]'}>{draft.activo ? 'Activo' : 'Inactivo'}</span></button>
            </div>
            <div className="px-6 py-4 border-t border-[#E2E8F0] flex justify-end gap-2"><button onClick={() => setEditing(null)} className="text-xs text-[#475569] px-4 py-2">Cancelar</button><button onClick={save} className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs px-5 py-2 rounded-lg">Guardar usuario</button></div>
          </div>
        </div>
      )}
    </div>
  )
}
