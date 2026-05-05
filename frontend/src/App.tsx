import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'

import AdminPage from '@/pages/AdminPage'
import ExamCtrlPage from '@/pages/ExamCtrlPage'
import LoginPage from '@/pages/LoginPage'
import StudentPage from '@/pages/StudentPage'
import TeacherPage from '@/pages/TeacherPage'
import TTOPage from '@/pages/TTOPage'
import RoomAvailabilityPage from '@/pages/RoomAvailabilityPage'
import UnauthorizedPage from '@/pages/UnauthorizedPage'
import AuthenticatedLayout from '@/components/AuthenticatedLayout'
import { useAuth } from '@/context/AuthContext'

const getDefaultRedirect = (roles: string[] | undefined): string => {
  if (roles?.includes('ROLE_ADMIN') || roles?.includes('ROLE_SUPER_ADMIN')) return '/admin'
  if (roles?.includes('ROLE_TTO')) return '/tto'
  if (roles?.includes('ROLE_TEACHER')) return '/teacher'
  if (roles?.includes('ROLE_EXAM_CONTROLLER')) return '/exam-ctrl'
  if (roles?.includes('ROLE_STUDENT')) return '/student'
  return '/login'
}

const hasAnyRole = (userRoles: string[] | undefined, allowedRoles: string[]) => {
  if (!userRoles) return false
  if (userRoles.includes('ROLE_ADMIN') || userRoles.includes('ROLE_SUPER_ADMIN')) return true
  return allowedRoles.some((role) => userRoles.includes(role))
}

const RequireRole = ({ allowedRoles, children }: { allowedRoles: string[]; children: JSX.Element }) => {
  const auth = useAuth()
  const location = useLocation()

  if (!auth.isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (!hasAnyRole(auth.user?.roles, allowedRoles)) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}

const HomeRedirect = () => {
  const auth = useAuth()

  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Navigate to={getDefaultRedirect(auth.user?.roles)} replace />
}

const withLayout = (page: JSX.Element) => <AuthenticatedLayout>{page}</AuthenticatedLayout>

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        <Route
          path="/student"
          element={
            <RequireRole allowedRoles={['ROLE_STUDENT']}>
              {withLayout(<StudentPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/teacher"
          element={
            <RequireRole allowedRoles={['ROLE_TEACHER']}>
              {withLayout(<TeacherPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/teacher/rooms"
          element={
            <RequireRole allowedRoles={['ROLE_TEACHER']}>
              {withLayout(<RoomAvailabilityPage userRole="TEACHER" />)}
            </RequireRole>
          }
        />
        <Route
          path="/tto"
          element={
            <RequireRole allowedRoles={['ROLE_TTO']}>
              {withLayout(<TTOPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/tto/rooms"
          element={
            <RequireRole allowedRoles={['ROLE_TTO']}>
              {withLayout(<RoomAvailabilityPage userRole="TTO" />)}
            </RequireRole>
          }
        />
        <Route
          path="/exam-ctrl"
          element={
            <RequireRole allowedRoles={['ROLE_EXAM_CONTROLLER']}>
              {withLayout(<ExamCtrlPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']}>
              {withLayout(<AdminPage />)}
            </RequireRole>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
