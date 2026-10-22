import { useState } from 'react'
import Login from './components/Login'
import NuevaCarga from './components/NuevaCarga'
import BandejaRevision from './components/BandejaRevision'
import Pedidos from './components/GestionPedidos'
import Clientes from './components/Clientes'
import Usuarios from './components/Usuarios'
import Auditoria from './components/Auditoria'
import { AppProvider, useApp } from './store'
import type { View } from './store'

const navItems: { icon: string; label: string; id: View }[] = [
  { icon: '⊞', label: 'Nueva carga',  id: 'nueva_carga' },
  { icon: '◈', label: 'Pedidos',      id: 'pedidos' },
  { icon: '⚑', label: 'En revisión',  id: 'revision' },
  { icon: '◉', label: 'Clientes',     id: 'clientes' },
  { icon: '♙', label: 'Usuarios',     id: 'usuarios' },
  { icon: '≋', label: 'Auditoría',    id: 'auditoria' },
]

function Shell({ onLogout }: { onLogout: () => void }) {
  const { view, setView, pedidos } = useApp()
  const [sidebarOpen, setSidebarOpen] = useState(true)

  const enRevisionCount = pedidos.filter(p => p.estado === 'en_revision').length

  const viewTitle: Record<View, string> = {
    dashboard:    'Panel',
    nueva_carga:  'Nueva carga',
    pedidos:      'Gestión de pedidos',
    revision:     'Bandeja — En Revisión',
    libreta:      'Clientes',
    clientes:     'Clientes',
    usuarios:     'Gestión de usuarios',
    auditoria:    'Auditoría global',
  }

  return (
    <div className="flex h-screen overflow-hidden bg-[#F8F9FA]">

      {/* Sidebar */}
      <aside
        className="flex flex-col bg-[#0F172A] border-r border-[#1E293B] transition-all duration-200 flex-shrink-0"
        style={{ width: sidebarOpen ? 200 : 56 }}
      >
        {/* Logo */}
        <button
          onClick={() => setView('dashboard')}
          className="flex items-center gap-3 px-4 py-5 border-b border-[#1E293B] hover:bg-[#1E293B] transition-colors w-full text-left"
        >
          <div className="w-7 h-7 rounded bg-[#2563EB] flex items-center justify-center flex-shrink-0">
            <svg width="16" height="12" viewBox="0 0 16 12" fill="none">
              <rect x="9" y="3" width="5" height="6" rx="1" fill="white" opacity="0.9"/>
              <rect x="10" y="4" width="3" height="2.5" rx="0.4" fill="#2563EB"/>
              <rect x="2" y="4" width="8" height="5" rx="0.8" fill="white"/>
              <circle cx="5" cy="10" r="1.4" fill="#2563EB"/>
              <circle cx="12" cy="10" r="1.4" fill="#2563EB"/>
              <circle cx="5" cy="10" r="0.6" fill="white"/>
              <circle cx="12" cy="10" r="0.6" fill="white"/>
            </svg>
          </div>
          {sidebarOpen && <span className="text-sm font-semibold tracking-wide text-white">MandáTodo</span>}
        </button>

        {/* Nav */}
        <nav className="flex-1 py-3 space-y-0.5 overflow-y-auto">
          {navItems.map(item => {
            const badge = item.id === 'revision' ? enRevisionCount : 0
            return (
              <button
                key={item.id}
                onClick={() => setView(item.id)}
                className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm transition-colors relative ${
                  view === item.id
                    ? 'bg-[#1E293B] text-white border-r-2 border-[#2563EB]'
                    : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'
                }`}
              >
                <span className="text-base flex-shrink-0 font-mono">{item.icon}</span>
                {sidebarOpen && (
                  <span className="font-medium flex-1 text-left">{item.label}</span>
                )}
                {badge > 0 && (
                  <span className={`flex-shrink-0 bg-[#FEF3C7] text-[#92400E] font-mono text-[9px] font-bold rounded-full px-1.5 py-0.5 ${sidebarOpen ? '' : 'absolute top-1.5 right-1.5'}`}>
                    {badge}
                  </span>
                )}
              </button>
            )
          })}
        </nav>

        {/* User */}
        <div className="p-3 border-t border-[#1E293B]">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-[#1E293B] flex items-center justify-center text-xs font-semibold text-[#BFDBFE] flex-shrink-0">A</div>
            {sidebarOpen && (
              <div className="min-w-0 flex-1">
                <div className="text-xs font-medium text-white truncate">Administrador</div>
                <button onClick={onLogout} className="block text-[11px] leading-tight text-[#94A3B8] hover:text-white transition-colors">Cerrar sesión</button>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">

        {/* Topbar */}
        <header className="flex items-center justify-between px-6 py-3.5 border-b border-[#E2E8F0] bg-[#FFFFFF] flex-shrink-0">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setSidebarOpen(v => !v)}
              className="text-[#64748B] hover:text-[#0F172A] transition-colors"
            >
              <svg width="16" height="16" fill="none" viewBox="0 0 16 16">
                <path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
            </button>
            <span className="text-sm font-medium text-[#0F172A]">{viewTitle[view]}</span>
          </div>
          <div className="flex items-center gap-4">
            {enRevisionCount > 0 && (
              <button
                onClick={() => setView('revision')}
                className="flex items-center gap-2 text-xs bg-[#FEF3C7] border border-[#92400E]/30 text-[#92400E] px-3 py-1.5 rounded-lg hover:bg-[#FDE68A] transition-colors"
              >
                <span>⚠</span>
                {enRevisionCount} en revisión
              </button>
            )}
            <button
              onClick={() => setView('nueva_carga')}
              className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-xs font-medium px-4 py-1.5 rounded-md transition-colors"
            >
              + Nueva carga
            </button>
            <div className="font-mono text-xs text-[#64748B]">
              {new Date().toLocaleDateString('es-AR', { day: '2-digit', month: 'short', year: 'numeric' })}
            </div>
          </div>
        </header>

        {/* View */}
        <div className="flex-1 flex overflow-hidden">
          {view === 'nueva_carga' && <NuevaCarga />}
          {view === 'pedidos'     && <Pedidos />}
          {view === 'revision'    && <BandejaRevision />}
          {view === 'clientes'    && <Clientes />}
          {view === 'usuarios'    && <Usuarios />}
          {view === 'auditoria'   && <Auditoria />}
          {(view === 'dashboard' || view === 'libreta') && <DashboardPlaceholder />}
        </div>
      </div>
    </div>
  )
}

function DashboardPlaceholder() {
  const { pedidos, clientes, setView } = useApp()
  const estados = { pendiente: 0, en_revision: 0, entregado: 0, fallido: 0 } as Record<string, number>
  pedidos.forEach(p => { estados[p.estado] = (estados[p.estado] || 0) + 1 })

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {[
            { label: 'Pendientes',  value: estados.pendiente,   color: 'text-[#0369A1]',  action: 'pedidos' as View },
            { label: 'En revisión', value: estados.en_revision, color: 'text-[#92400E]',  action: 'revision' as View },
            { label: 'Entregados',  value: estados.entregado,   color: 'text-[#15803D]',  action: 'pedidos' as View },
            { label: 'Clientes',    value: clientes.length,     color: 'text-[#2563EB]',  action: 'clientes' as View },
          ].map(k => (
            <button key={k.label} onClick={() => setView(k.action)} className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg p-4 text-left shadow-sm hover:border-[#BFDBFE] transition-colors">
              <div className={`font-mono text-2xl font-medium ${k.color}`}>{k.value}</div>
              <div className="text-xs text-[#64748B] mt-1">{k.label}</div>
            </button>
          ))}
        </div>
        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-8 text-center shadow-sm">
          <button
            onClick={() => setView('nueva_carga')}
            className="bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm font-medium px-8 py-3 rounded-md transition-colors"
          >
            + Registrar nuevo pedido
          </button>
        </div>
      </div>
    </div>
  )
}

function AppContent() {
  const [logged, setLogged] = useState(false)
  if (!logged) return <Login onLogin={() => setLogged(true)} />
  return <Shell onLogout={() => setLogged(false)} />
}

export default function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  )
}
