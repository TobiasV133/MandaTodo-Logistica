import { Navigate, Route, Routes } from 'react-router-dom'

import AppLayout from './components/layout/AppLayout'
import AdminRoute from './components/layout/AdminRoute'
import ProtectedRoute from './components/layout/ProtectedRoute'
import AuditoriaView from './pages/AuditoriaView'
import ClientesView from './pages/ClientesView'
import DashboardView from './pages/DashboardView'
import LoginView from './pages/LoginView'
import NuevaCargaView from './pages/NuevaCargaView'
import PedidosView from './pages/PedidosView'
import UsuariosView from './pages/UsuariosView'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginView />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardView />} />
          <Route path="dashboard" element={<DashboardView />} />
          <Route path="nueva-carga" element={<NuevaCargaView />} />
          <Route path="pedidos" element={<PedidosView />} />
          <Route path="en-revision" element={<PedidosView soloRevision />} />
          <Route path="clientes" element={<ClientesView />} />
          <Route element={<AdminRoute />}>
            <Route path="usuarios" element={<UsuariosView />} />
            <Route path="auditoria" element={<AuditoriaView />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
