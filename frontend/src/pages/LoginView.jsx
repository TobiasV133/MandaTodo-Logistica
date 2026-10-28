import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'

import Icon from '../components/ui/Icon'
import { useAuth } from '../context/AuthContext'
import { getApiErrorMessage } from '../services/api'

export default function LoginView() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const destino = location.state?.from?.pathname || '/'

  if (isAuthenticated) {
    return <Navigate to={destino} replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (!email.trim() || !password) {
      setError('Ingresá tu email y contraseña para continuar.')
      return
    }

    setLoading(true)
    try {
      await login({ username: email.trim(), password })
      navigate(destino, { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'No se pudo iniciar sesión. Intentá nuevamente.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-glow auth-glow--one" />
      <div className="auth-glow auth-glow--two" />

      <main className="auth-card">
        <div className="auth-brand">
          <span className="auth-logo">
            <Icon name="package" size={27} strokeWidth={1.9} />
          </span>
          <div>
            <div className="auth-brand__name">
              Mandá<span>Todo</span>
            </div>
            <div className="auth-brand__tagline">Logística urbana</div>
          </div>
        </div>

        <div className="auth-heading">
          <h1>Acceso al sistema</h1>
          <p>Ingresá con tu email y contraseña de operario.</p>
        </div>

        {error && (
          <div className="alert alert-danger auth-alert" role="alert" aria-live="polite">
            <Icon name="alert" size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label className="form-label field-label" htmlFor="login-email">
              Email
            </label>
            <div className="input-with-icon">
              <Icon name="mail" size={18} />
              <input
                id="login-email"
                type="email"
                className="form-control"
                placeholder="laura.gomez@mandatodo.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                autoFocus
                disabled={loading}
              />
            </div>
          </div>

          <div className="mb-4">
            <label className="form-label field-label" htmlFor="login-password">
              Contraseña
            </label>
            <div className="input-with-icon">
              <Icon name="lock" size={18} />
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                className="form-control"
                placeholder="Tu contraseña"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                disabled={loading}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((value) => !value)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                <Icon name={showPassword ? 'eyeOff' : 'eye'} size={18} />
              </button>
            </div>
          </div>

          <button type="submit" className="btn btn-primary auth-submit" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                Ingresando…
              </>
            ) : (
              <>
                Ingresar
                <Icon name="arrowRight" size={18} />
              </>
            )}
          </button>
        </form>

        <p className="auth-help">Usá el email y la contraseña asignados por el administrador.</p>

        <footer className="auth-footer">Sistema interno · Acceso restringido</footer>
      </main>
    </div>
  )
}
