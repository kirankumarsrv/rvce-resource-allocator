import { ReactNode, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { logout as logoutBackend } from '@/services/authService'

interface AuthenticatedLayoutProps {
  children: ReactNode
}

const AuthenticatedLayout = ({ children }: AuthenticatedLayoutProps) => {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const activePath = location.pathname
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  interface NavItem {
    label: string
    path: string
    icon: JSX.Element
  }

  const navItems = useMemo(() => {
    const links: NavItem[] = []
    const hasRole = (role: string) => auth.user?.roles?.includes(role)
    const addLink = (link: NavItem) => {
      if (!links.some((item) => item.path === link.path)) {
        links.push(link)
      }
    }

    const icons = {
      dashboard: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 13h8V3H3v10z" />
          <path d="M13 21h8V11h-8v10z" />
        </svg>
      ),
      upload: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 17v2a2 2 0 002 2h12a2 2 0 002-2v-2" />
          <path d="M7 11l5-5 5 5" />
          <path d="M12 6v11" />
        </svg>
      ),
      substitute: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          <path d="M12 6V2l-4 4 4 4V8c4.42 0 8 1.79 8 4v1" />
          <path d="M8 20.87a4 4 0 0 1 0-7.75" />
          <path d="M12 18v4l4-4-4-4v3c-4.42 0-8-1.79-8-4v-1" />
        </svg>
      ),
      override: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          <path d="M9 11l2 2 4-4" />
        </svg>
      ),
      room: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 21V8a2 2 0 012-2h14a2 2 0 012 2v13" />
          <path d="M16 3v4" />
          <path d="M8 3v4" />
        </svg>
      ),
      reservation: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M8 3v4" />
          <path d="M16 3v4" />
          <path d="M3 11h18" />
        </svg>
      ),
      exam: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 6h16" />
          <path d="M4 10h16" />
          <path d="M5 18h14" />
          <path d="M6 14h.01" />
          <path d="M10 14h.01" />
          <path d="M14 14h.01" />
          <path d="M18 14h.01" />
        </svg>
      ),
      student: (
        <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 14c4.418 0 8 1.79 8 4v2H4v-2c0-2.21 3.582-4 8-4z" />
          <path d="M12 2L4 5l8 3 8-3-8-3z" />
        </svg>
      ),
    }

    if (hasRole('ROLE_TTO')) {
      addLink({ label: 'TTO Dashboard', path: '/tto', icon: icons.dashboard })
      addLink({ label: 'Upload Timetable', path: '/tto/upload', icon: icons.upload })
      addLink({ label: 'Substitute Teacher', path: '/tto/substitute', icon: icons.substitute })
      addLink({ label: 'Room Availability', path: '/tto/rooms', icon: icons.room })
    }

    if (hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPER_ADMIN')) {
      addLink({ label: 'Admin Dashboard', path: '/admin', icon: icons.dashboard })
      addLink({ label: 'Substitute Teacher', path: '/admin/substitute', icon: icons.substitute })
      addLink({ label: 'Room Availability', path: '/tto/rooms', icon: icons.room })
    }

    if (hasRole('ROLE_TEACHER')) {
      addLink({ label: 'Teacher Dashboard', path: '/teacher', icon: icons.dashboard })
      addLink({ label: 'Room Availability', path: '/teacher/rooms', icon: icons.room })
    }

    if (hasRole('ROLE_EXAM_CONTROLLER') || hasRole('ROLE_DEPT_COORD')) {
      addLink({ label: 'Exam Control', path: '/exam-ctrl', icon: icons.exam })
      addLink({ label: 'Room Availability', path: '/dept-coord/rooms', icon: icons.room })
    }

    if (hasRole('ROLE_STUDENT')) {
      addLink({ label: 'Student Dashboard', path: '/student', icon: icons.student })
    }

    return links
  }, [auth.user?.roles])

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
              data-test-id="logout-button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="rounded-2xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoggingOut ? 'Logging out…' : 'Logout'}
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between lg:hidden mb-4">
          <div>
            <p className="text-sm font-semibold text-slate-700">Quick Menu</p>
            <p className="text-xs text-slate-500">Tap to open sidebar navigation</p>
          </div>
          <button
            type="button"
            onClick={() => setIsSidebarOpen((open) => !open)}
            className="rounded-2xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            {isSidebarOpen ? 'Hide Menu' : 'Show Menu'}
          </button>
        </div>

        <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className={`${isSidebarOpen ? 'block' : 'hidden'} rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:block lg:sticky lg:top-6 lg:self-start`}>
            <div className="mb-6 flex items-start justify-between gap-4 lg:block">
              <div>
                <p className="text-sm font-semibold uppercase tracking-wide text-slate-500">Navigation</p>
                <p className="mt-2 text-sm text-slate-600">Quick access to timetable tools.</p>
              </div>
              <button
                type="button"
                onClick={() => setIsSidebarOpen(false)}
                className="lg:hidden rounded-full bg-slate-100 p-2 text-slate-700 hover:bg-slate-200"
              >
                <span className="sr-only">Close menu</span>
                ✕
              </button>
            </div>
            <nav className="space-y-2">
              {navItems.map((item) => {
                const isActive = activePath === item.path || activePath.startsWith(`${item.path}/`)
                return (
                  <Link
                    key={item.path}
                    data-test-id={`nav-${item.path.replace(/[^a-z0-9]/gi, '-')}`}
                    to={item.path}
                    onClick={() => setIsSidebarOpen(false)}
                    className={`flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium transition ${
                      isActive
                        ? 'bg-slate-900 text-white shadow-sm'
                        : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                    }`}
                  >
                    <span className="text-slate-400">{item.icon}</span>
                    <span>{item.label}</span>
                  </Link>
                )
              })}
            </nav>
          </aside>

          <section>{children}</section>
        </div>
      </main>
    </div>
  )
}

export default AuthenticatedLayout
