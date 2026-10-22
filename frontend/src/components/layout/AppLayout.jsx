import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

import HeaderBar from './HeaderBar'
import Sidebar from './Sidebar'
import { ResumenProvider } from '../../context/ResumenContext'

const TITULOS = {
  '/': 'Panel de control',
  '/dashboard': 'Panel de control',
  '/nueva-carga': 'Nueva carga',
  '/pedidos': 'Pedidos',
  '/en-revision': 'En revisión',
  '/clientes': 'Clientes',
  '/usuarios': 'Usuarios',
  '/auditoria': 'Auditoría',
}

function LayoutShell() {
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const title = TITULOS[location.pathname] ?? 'MandáTodo'

  return (
    <div className="app-shell">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {sidebarOpen && (
        <button
          type="button"
          className="sidebar-overlay d-lg-none"
          aria-label="Cerrar menú"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className="app-main">
        <HeaderBar title={title} onOpenMenu={() => setSidebarOpen(true)} />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default function AppLayout() {
  return (
    <ResumenProvider>
      <LayoutShell />
    </ResumenProvider>
  )
}
