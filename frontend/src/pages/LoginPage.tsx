import { FormEvent, useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

const getDefaultRedirect = (roles: string[] | undefined): string => {
  if (!roles?.length) return '/student'
  if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) return '/admin'
  if (roles.includes('ROLE_TTO')) return '/tto'
  if (roles.includes('ROLE_DEPT_COORD') || roles.includes('ROLE_EXAM_CONTROLLER')) return '/exam-ctrl'
  if (roles.includes('ROLE_TEACHER')) return '/teacher'
  if (roles.includes('ROLE_STUDENT')) return '/student'
  return '/student'
}

const LoginPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const from = (location.state as { from?: Location })?.from?.pathname ?? '/'

  useEffect(() => {
    document.title = 'Login | RVCE Resource Allocator'
  }, [])

  if (auth.isAuthenticated) {
    return <Navigate to={from === '/' ? getDefaultRedirect(auth.user?.roles) : from} replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const user = await auth.login({ email, password })
      const destination = from === '/' ? getDefaultRedirect(user?.roles) : from
      navigate(destination, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed. Check credentials.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-8 shadow-lg shadow-slate-200/50">
        <h1 className="mb-4 text-3xl font-semibold text-slate-900">RVCE SCAS Login</h1>
        <p className="mb-6 text-sm text-slate-600">Enter your institution credentials to continue.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-slate-700">
              Email address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="teacher@rvce.edu.in"
              required
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-slate-700">
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="••••••••"
              required
            />
          </div>

          {error ? <div className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div> : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-3 w-full rounded-2xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="mt-6 text-sm text-slate-500">
          Use your RVCE email and password. If you do not have access, contact the SCAS administrator.
        </p>
      </div>
    </div>
  )
}

export default LoginPage
