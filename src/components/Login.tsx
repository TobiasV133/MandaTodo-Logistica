import { useState } from 'react'

interface Props { onLogin: () => void }

export default function Login({ onLogin }: Props) {
  const [user, setUser] = useState('')
  const [pass, setPass] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!user || !pass) { setError('Completá usuario y contraseña.'); return }
    setLoading(true)
    setTimeout(() => {
      if (user === 'admin' && pass === 'admin') {
        onLogin()
      } else {
        setError('Credenciales incorrectas. Intentá con admin / admin.')
        setLoading(false)
      }
    }, 600)
  }

  return (
    <div className="min-h-screen bg-[#F8F9FA] flex items-center justify-center p-4">
      <div className="w-full max-w-sm">

        {/* Logo */}
        <div className="flex items-center justify-center gap-3 mb-10">
          <div className="w-9 h-9 rounded-lg bg-[#2563EB] flex items-center justify-center">
            <svg width="20" height="14" viewBox="0 0 20 14" fill="none">
              <rect x="10" y="3" width="7" height="7" rx="1" fill="white" opacity="0.9"/>
              <rect x="11.5" y="4.2" width="4" height="3" rx="0.5" fill="#2563EB"/>
              <rect x="2" y="4.5" width="10" height="6" rx="1" fill="white"/>
              <circle cx="6" cy="12" r="1.8" fill="#2563EB"/>
              <circle cx="15" cy="12" r="1.8" fill="#2563EB"/>
              <circle cx="6" cy="12" r="0.8" fill="white"/>
              <circle cx="15" cy="12" r="0.8" fill="white"/>
            </svg>
          </div>
          <span className="text-lg font-semibold text-[#0F172A] tracking-wide">MandáTodo</span>
        </div>

        <div className="bg-[#FFFFFF] border border-[#E2E8F0] rounded-xl p-8 shadow-sm">
          <h1 className="text-base font-semibold text-[#0F172A] mb-1">Acceso al sistema</h1>
          <p className="text-xs text-[#64748B] mb-6">Panel de operaciones internas</p>

          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#64748B] mb-1.5">Usuario</label>
              <input
                type="text"
                value={user}
                onChange={e => setUser(e.target.value)}
                placeholder="admin"
                autoComplete="username"
                className="w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2.5 text-sm text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-[#64748B] mb-1.5">Contraseña</label>
              <input
                type="password"
                value={pass}
                onChange={e => setPass(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
                className="w-full bg-[#FFFFFF] border border-[#E2E8F0] rounded-lg px-3 py-2.5 text-sm text-[#0F172A] placeholder-[#64748B] focus:outline-none focus:border-[#2563EB] transition-colors"
              />
            </div>

            {error && (
              <div className="text-xs text-[#B91C1C] bg-[#FEE2E2] border border-[#B91C1C]/20 rounded-lg px-3 py-2">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-sm font-medium py-2.5 rounded-md transition-colors mt-2"
            >
              {loading ? 'Verificando...' : 'Ingresar'}
            </button>
          </form>
        </div>

        <p className="text-center text-[10px] text-[#64748B] mt-6 font-mono">
          Sistema interno · Acceso restringido
        </p>
      </div>
    </div>
  )
}
