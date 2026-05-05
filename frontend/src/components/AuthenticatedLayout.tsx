import { ReactNode, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { logout as logoutBackend } from '@/services/authService'

interface AuthenticatedLayoutProps {
  children: ReactNode
}

const AuthenticatedLayout = ({ children }: AuthenticatedLayoutProps) => {
  const auth = useAuth()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logoutBackend()
    } catch {
      // Even if backend logout fails, clear local auth state so the user is signed out.
    } finally {
      auth.logout()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <div>
            <p className="text-lg font-semibold text-slate-900">RVCE SCAS Portal</p>
            <p className="text-sm text-slate-600">
              Signed in as <span className="font-medium text-slate-900">{auth.user?.email ?? 'unknown'}</span>
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {auth.user?.roles && auth.user.roles.length > 0 && (
              <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-700">
                {auth.user.roles.join(', ')}
              </span>
            )}
            <button
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="rounded-2xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoggingOut ? 'Logging out…' : 'Logout'}
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</main>
    </div>
  )
}

export default AuthenticatedLayout
