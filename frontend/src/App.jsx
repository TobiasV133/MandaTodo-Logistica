import { Navigate, Route, Routes } from 'react-router-dom'

import AppLayout from './components/layout/AppLayout'
import ProtectedRoute from './components/layout/ProtectedRoute'
import AuditoriaPage from './pages/AuditoriaPage'
import ClientesPage from './pages/ClientesPage'
import DashboardView from './pages/DashboardView'
import LoginView from './pages/LoginView'
import NuevaCargaView from './pages/NuevaCargaView'
import OperariosPage from './pages/OperariosPage'
import PedidosView from './pages/PedidosView'

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
          <Route path="clientes" element={<ClientesPage />} />
          <Route path="operarios" element={<OperariosPage />} />
          <Route path="usuarios" element={<OperariosPage />} />
          <Route path="auditoria" element={<AuditoriaPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
