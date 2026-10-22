import React, { useState } from 'react'
import { useApp } from '../store'
import { MOTIVOS_FALLA } from '../types'
import type { EstadoPedido, MotivoFalla } from '../types'

const ESTADOS: { value: EstadoPedido | 'todos'; label: string; color: string }[] = [
  { value: 'todos',       label: 'Todos',        color: 'text-[#e8eaf0]' },
  { value: 'pendiente',   label: 'Pendiente',    color: 'text-[#38bdf8]' },
  { value: 'en_revision', label: 'En revisión',  color: 'text-[#f5a623]' },
  { value: 'entregado',   label: 'Entregado',    color: 'text-[#22d07a]' },
  { value: 'fallido',     label: 'Fallido',      color: 'text-[#ff4560]' },
]

const DOT: Record<EstadoPedido, string> = {
  pendiente:   'bg-[#38bdf8]',
  en_revision: 'bg-[#f5a623]',
  en_transito: 'bg-[#a78bfa]',
  entregado:   'bg-[#22d07a]',
  fallido:     'bg-[#ff4560]',
  cancelado_transito: 'bg-[#8B5CF6]',
}

const inCls = 'w-full bg-[#0a0d14] border border-[#1e2740] rounded px-2.5 py-1.5 text-xs text-[#e8eaf0] placeholder-[#6b7694] focus:outline-none focus:border-[#8B5CF6] transition-colors'

export default function Pedidos() {
  const { pedidos, clientes, updatePedidoEstado, updatePedido } = useApp()
  const [filterEstado, setFilterEstado] = useState<EstadoPedido | 'todos'>('todos')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<string | null>(null)
  const [viewingAddr, setViewingAddr] = useState<string | null>(null)

  // estado change
  const [newEstado, setNewEstado] = useState<EstadoPedido>('entregado')
  const [motivo, setMotivo] = useState<MotivoFalla>('Ausente')
  const [nota, setNota] = useState('')

  // data edit
  const [editDesc, setEditDesc] = useState('')
  const [editPeso, setEditPeso] = useState('')
  const [editMonto, setEditMonto] = useState('')
  const [editKm, setEditKm] = useState('')
  const [editFecha, setEditFecha] = useState('')
  const [editTab, setEditTab] = useState<'datos' | 'estado'>('datos')

  const counts: Record<string, number> = {
    todos: pedidos.length,
    pendiente:   pedidos.filter(p => p.estado === 'pendiente').length,
    en_revision: pedidos.filter(p => p.estado === 'en_revision').length,
    entregado:   pedidos.filter(p => p.estado === 'entregado').length,
    fallido:     pedidos.filter(p => p.estado === 'fallido').length,
  }

  function getCliente(id: string) { return clientes.find(c => c.id === id) }

  const byEstado = filterEstado === 'todos' ? pedidos : pedidos.filter(p => p.estado === filterEstado)

  const bySearch = search.trim()
    ? byEstado.filter(p => {
        const q = search.toLowerCase()
        const c = getCliente(p.clienteId)
        return (
          p.id.toLowerCase().includes(q) ||
          p.descripcion.toLowerCase().includes(q) ||
          c?.nombre.toLowerCase().includes(q) ||
          c?.documento.replace(/-/g, '').includes(q.replace(/-/g, ''))
        )
      })
    : byEstado

  const sorted = [...bySearch].sort((a, b) => b.actualizadoEn.localeCompare(a.actualizadoEn))

  function openEdit(pedidoId: string) {
    const p = pedidos.find(x => x.id === pedidoId)!
    setEditing(pedidoId)
    setEditDesc(p.descripcion)
    setEditPeso(p.peso)
    setEditMonto(p.monto)
    setEditKm(p.kilometros ?? '')
    setEditFecha(p.fechaEntrega ?? '')
    setNewEstado('entregado')
    setMotivo('Ausente')
    setNota('')
    setEditTab('datos')
  }

  function applyDatos(pedidoId: string) {
    updatePedido(pedidoId, { descripcion: editDesc, peso: editPeso, monto: editMonto, kilometros: editKm, fechaEntrega: editFecha })
    setEditing(null)
  }

  function applyEstado(pedidoId: string) {
    updatePedidoEstado(pedidoId, newEstado, newEstado === 'fallido' ? motivo : undefined, newEstado === 'fallido' ? nota : undefined)
    setEditing(null)
    setNota('')
  }

  const activeEstado = ESTADOS.find(e => e.value === filterEstado)!

  return (
    <div className="flex-1 flex flex-col overflow-hidden">

      {/* Header + filters */}
      <div className="px-6 pt-5 pb-0 flex-shrink-0 space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-[#e8eaf0]">Gestión de pedidos</h2>
            <p className="text-xs text-[#6b7694] mt-0.5">
              {sorted.length} pedido{sorted.length !== 1 ? 's' : ''} · <span className={activeEstado.color}>{activeEstado.label}</span>
            </p>
          </div>
        </div>

        {/* Search */}
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Buscar por código, nombre, DNI/CUIT o descripción..."
          className="w-full bg-[#111520] border border-[#1e2740] rounded-lg px-3 py-2 text-sm text-[#e8eaf0] placeholder-[#6b7694] focus:outline-none focus:border-[#8B5CF6] transition-colors"
        />

        {/* Filter tabs */}
        <div className="flex items-center gap-0 bg-[#111520] border border-[#1e2740] rounded-lg p-1">
          {ESTADOS.map(e => (
            <button
              key={e.value}
              onClick={() => { setFilterEstado(e.value); setEditing(null) }}
              className={`flex-1 flex items-center justify-center gap-1.5 text-xs py-1.5 rounded transition-colors ${
                filterEstado === e.value ? 'bg-[#8B5CF6] text-white' : 'text-[#6b7694] hover:text-[#e8eaf0]'
              }`}
            >
              <span>{e.label}</span>
              <span className={`font-mono text-[10px] ${filterEstado === e.value ? 'opacity-80' : 'opacity-50'}`}>{counts[e.value]}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto px-6 py-4">
        <div className="bg-[#111520] border border-[#1e2740] rounded-xl" style={{ minWidth: 820 }}>
          {sorted.length === 0 ? (
            <div className="p-12 text-center text-xs text-[#6b7694]">
              {search ? 'Sin resultados para esa búsqueda.' : 'No hay pedidos en este estado.'}
            </div>
          ) : (
            <table className="w-full text-sm" style={{ tableLayout: 'fixed' }}>
              <colgroup>
                <col style={{ width: 80 }} />
                <col style={{ width: 140 }} />
                <col />
                <col style={{ width: 110 }} />
                <col style={{ width: 110 }} />
                <col style={{ width: 90 }} />
                <col style={{ width: 90 }} />
                <col style={{ width: 130 }} />
              </colgroup>
              <thead>
                <tr className="border-b border-[#1e2740]">
                  {['ID', 'Cliente', 'Descripción', 'Peso / Monto', 'Estado', 'Creado', 'Entrega', ''].map((h, i) => (
                    <th key={i} className="px-4 py-3 text-left text-[10px] font-mono font-medium text-[#6b7694] uppercase tracking-wider whitespace-nowrap overflow-hidden">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {sorted.map(p => {
                  const cliente = getCliente(p.clienteId)
                  const isEditing = editing === p.id
                  const canEdit = p.estado !== 'entregado' && p.estado !== 'fallido'
                  return (
                    <React.Fragment key={p.id}>
                      <tr className={`border-b border-[#1e2740] hover:bg-[#161c2d] transition-colors ${isEditing ? 'bg-[#161c2d]' : ''}`}>
                        <td className="px-4 py-3 font-mono text-xs text-[#38bdf8] whitespace-nowrap">{p.id}</td>
                        <td className="px-4 py-3 overflow-hidden">
                          <div className="text-xs text-[#e8eaf0] truncate">{cliente?.nombre || '—'}</div>
                          <div className="font-mono text-[10px] text-[#6b7694]">{cliente?.tipoCliente}</div>
                        </td>
                        <td className="px-4 py-3 text-xs text-[#c0c8e0] overflow-hidden">
                          <div className="truncate">{p.descripcion}</div>
                          {p.alertas.length > 0 && <div className="text-[10px] text-[#f5a623] mt-0.5">⚠ {p.alertas.length} alerta{p.alertas.length > 1 ? 's' : ''}</div>}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <div className="text-xs text-[#6b7694]">{p.peso || '—'}</div>
                          <div className="font-mono text-xs text-[#e8eaf0]">{p.monto ? `$${p.monto}` : '—'}</div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <div className="flex items-center gap-1.5">
                            <div className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${DOT[p.estado]}`} />
                            <span className={`text-xs ${ESTADOS.find(e => e.value === p.estado)?.color || 'text-[#6b7694]'}`}>
                              {ESTADOS.find(e => e.value === p.estado)?.label}
                            </span>
                          </div>
                          {p.motivoFalla && <div className="text-[10px] text-[#ff4560] mt-0.5">{p.motivoFalla}</div>}
                        </td>
                        <td className="px-4 py-3 font-mono text-xs text-[#6b7694] whitespace-nowrap">{p.creadoEn}</td>
                        <td className="px-4 py-3 font-mono text-xs whitespace-nowrap">
                          {p.fechaEntrega
                            ? <span className="text-[#e8eaf0]">{p.fechaEntrega}</span>
                            : <span className="text-[#6b7694]">—</span>}
                        </td>
                        <td className="px-3 py-3">
                          <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                            <button
                              onClick={() => setViewingAddr(viewingAddr === p.id ? null : p.id)}
                              style={{ flexShrink: 0 }}
                              className={`text-xs px-2 py-1 rounded border transition-colors ${
                                viewingAddr === p.id ? 'border-[#8B5CF6]/40 bg-[#8B5CF6]/10 text-[#8B5CF6]' : 'border-[#1e2740] text-[#6b7694] hover:text-[#e8eaf0]'
                              }`}
                            >
                              Dir.
                            </button>
                            {canEdit && (
                              <button
                                onClick={() => isEditing ? setEditing(null) : openEdit(p.id)}
                                style={{ flexShrink: 0 }}
                                className={`text-xs px-2 py-1 rounded border transition-colors ${
                                  isEditing ? 'border-[#1e2740] text-[#6b7694]' : 'border-[#8B5CF6]/40 text-[#8B5CF6] hover:bg-[#8B5CF6]/10'
                                }`}
                              >
                                {isEditing ? 'Cerrar' : 'Editar'}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>

                      {isEditing && (
                        <tr className="bg-[#0d1020] border-b border-[#1e2740]">
                          <td colSpan={8} className="px-6 py-5">
                            {/* Edit tabs */}
                            <div className="flex gap-1 mb-4 bg-[#111520] border border-[#1e2740] rounded-lg p-1 w-fit">
                              <button
                                onClick={() => setEditTab('datos')}
                                className={`text-xs px-4 py-1.5 rounded transition-colors ${editTab === 'datos' ? 'bg-[#8B5CF6] text-white' : 'text-[#6b7694] hover:text-[#e8eaf0]'}`}
                              >
                                Editar datos
                              </button>
                              <button
                                onClick={() => setEditTab('estado')}
                                className={`text-xs px-4 py-1.5 rounded transition-colors ${editTab === 'estado' ? 'bg-[#8B5CF6] text-white' : 'text-[#6b7694] hover:text-[#e8eaf0]'}`}
                              >
                                Cambiar estado
                              </button>
                            </div>

                            {editTab === 'datos' && (
                              <div className="flex items-end gap-4 flex-wrap">
                                <div className="flex-1 min-w-48">
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Descripción</div>
                                  <input value={editDesc} onChange={e => setEditDesc(e.target.value)} className={inCls} />
                                </div>
                                <div>
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Peso</div>
                                  <input value={editPeso} onChange={e => setEditPeso(e.target.value)} placeholder="Ej: 5 kg" className={`${inCls} w-28`} />
                                </div>
                                <div>
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Monto ($)</div>
                                  <input value={editMonto} onChange={e => setEditMonto(e.target.value)} placeholder="Ej: 25000" className={`${inCls} w-28`} />
                                </div>
                                <div>
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Kilómetros</div>
                                  <input value={editKm} onChange={e => setEditKm(e.target.value)} placeholder="Ej: 45" className={`${inCls} w-24`} />
                                </div>
                                <div>
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Fecha de entrega</div>
                                  <input type="date" value={editFecha} onChange={e => setEditFecha(e.target.value)} className={`${inCls} w-36`} />
                                </div>
                                <button
                                  onClick={() => applyDatos(p.id)}
                                  disabled={!editDesc}
                                  className="bg-[#8B5CF6] disabled:opacity-40 hover:bg-[#7C3AED] text-white text-xs px-4 py-1.5 rounded transition-colors"
                                >
                                  Guardar
                                </button>
                              </div>
                            )}

                            {editTab === 'estado' && (
                              <div className="flex items-start gap-4 flex-wrap">
                                <div>
                                  <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Nuevo estado</div>
                                  <div className="flex gap-1.5">
                                    {(['pendiente', 'entregado', 'fallido'] as EstadoPedido[]).map(s => (
                                      <button
                                        key={s}
                                        onClick={() => setNewEstado(s)}
                                        className={`text-xs px-3 py-1.5 rounded border transition-colors ${newEstado === s ? 'bg-[#161c2d] border-[#8B5CF6] text-white' : 'border-[#1e2740] text-[#6b7694] hover:text-[#e8eaf0]'}`}
                                      >
                                        {ESTADOS.find(e => e.value === s)?.label}
                                      </button>
                                    ))}
                                  </div>
                                </div>
                                {newEstado === 'fallido' && (
                                  <div>
                                    <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Motivo <span className="text-[#ff4560]">*</span></div>
                                    <select
                                      value={motivo}
                                      onChange={e => setMotivo(e.target.value as MotivoFalla)}
                                      className="bg-[#0a0d14] border border-[#1e2740] rounded text-xs text-[#e8eaf0] px-2.5 py-1.5 focus:outline-none focus:border-[#8B5CF6]"
                                    >
                                      {MOTIVOS_FALLA.map(m => <option key={m}>{m}</option>)}
                                    </select>
                                  </div>
                                )}
                                {newEstado === 'fallido' && (
                                  <div className="flex-1 min-w-48">
                                    <div className="text-[10px] font-mono text-[#6b7694] uppercase mb-1.5">Nota (opcional)</div>
                                    <input
                                      value={nota}
                                      onChange={e => setNota(e.target.value)}
                                      placeholder="Detalles adicionales..."
                                      className={inCls}
                                    />
                                  </div>
                                )}
                                <div className="flex items-end">
                                  <button
                                    onClick={() => applyEstado(p.id)}
                                    className="bg-[#8B5CF6] hover:bg-[#7C3AED] text-white text-xs px-4 py-1.5 rounded transition-colors"
                                  >
                                    Aplicar
                                  </button>
                                </div>
                              </div>
                            )}
                          </td>
                        </tr>
                      )}
                      {viewingAddr === p.id && (() => {
                        const cli = getCliente(p.clienteId)
                        const dir = cli?.direcciones.find(d => d.id === p.direccionId) ?? cli?.direcciones[0]
                        return (
                          <tr className="bg-[#0a0e1a] border-b border-[#1e2740]">
                            <td colSpan={8} className="px-6 py-4" style={{ borderTop: '1px solid #1e2740' }}>
                              <div className="text-[10px] font-mono text-[#6b7694] uppercase tracking-wider mb-2">Dirección de entrega</div>
                              {dir ? (
                                <div className="grid grid-cols-3 gap-x-6 gap-y-1 text-xs">
                                  <div><span className="text-[#6b7694]">Calle: </span><span className="text-[#e8eaf0]">{dir.calle || '—'}</span></div>
                                  <div><span className="text-[#6b7694]">Altura: </span><span className="text-[#e8eaf0]">{dir.altura || 'S/N'}</span></div>
                                  {dir.piso && <div><span className="text-[#6b7694]">Piso/Dpto: </span><span className="text-[#e8eaf0]">{dir.piso}{dir.depto ? ` · ${dir.depto}` : ''}</span></div>}
                                  <div><span className="text-[#6b7694]">Localidad: </span><span className="text-[#e8eaf0]">{dir.localidad || '—'}</span></div>
                                  <div><span className="text-[#6b7694]">Provincia: </span><span className="text-[#e8eaf0]">{dir.provincia || '—'}</span></div>
                                  {dir.lat && <div className="font-mono text-[#6b7694]">{dir.lat.toFixed(5)}, {dir.lng?.toFixed(5)}</div>}
                                </div>
                              ) : (
                                <span className="text-xs text-[#6b7694]">Sin dirección registrada.</span>
                              )}
                            </td>
                          </tr>
                        )
                      })()}
                    </React.Fragment>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
