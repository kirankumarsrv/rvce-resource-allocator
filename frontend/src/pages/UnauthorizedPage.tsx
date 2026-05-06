import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

const UnauthorizedPage = () => {
  const navigate = useNavigate()
  const auth = useAuth()

  const handleBackHome = () => {
    // Redirect to appropriate dashboard based on user role
    if (auth.user?.roles?.includes('ROLE_ADMIN')) navigate('/admin', { replace: true })
    else if (auth.user?.roles?.includes('ROLE_TTO')) navigate('/tto', { replace: true })
    else if (auth.user?.roles?.includes('ROLE_DEPT_COORD')) navigate('/exam-ctrl', { replace: true })
    else if (auth.user?.roles?.includes('ROLE_TEACHER')) navigate('/teacher', { replace: true })
    else if (auth.user?.roles?.includes('ROLE_STUDENT')) navigate('/student', { replace: true })
    else navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-xl rounded-3xl border border-slate-200 bg-white p-10 shadow-lg shadow-slate-200/50 text-center">
        <h1 className="text-4xl font-bold text-slate-900 mb-4">Access Denied</h1>
        <p className="text-slate-600 mb-6">
          Your account does not have permission to view this page. Please contact the administrator or sign in with a different account.
        </p>
        <button
          onClick={handleBackHome}
          className="inline-flex items-center justify-center rounded-2xl bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          Back to Home
        </button>
      </div>
    </div>
  )
}

export default UnauthorizedPage
