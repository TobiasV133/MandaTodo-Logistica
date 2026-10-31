import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'leaflet/dist/leaflet.css'
import './index.css'
import './utils/leafletSetup'

import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { TarifaProvider } from './context/TarifaContext.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <TarifaProvider>
          <App />
        </TarifaProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
