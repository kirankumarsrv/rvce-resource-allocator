import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'

import AdminPage from '@/pages/AdminPage'
import ExamCtrlPage from '@/pages/ExamCtrlPage'
import CreateExamPage from '@/pages/CreateExamPage'
import { SeatingDashboard } from '@/pages/SeatingDashboardPage'
import LoginPage from '@/pages/LoginPage'
import ForgotPasswordPage from '@/pages/ForgotPasswordPage'
import ResetPasswordPage from '@/pages/ResetPasswordPage'
import StudentPage from '@/pages/StudentPage'
import TeacherPage from '@/pages/TeacherPage'
import TTOPage from '@/pages/TTOPage'
import UploadTimetablePage from '@/pages/UploadTimetablePage'
import SubstitutePage from '@/pages/SubstitutePage'
import RoomAvailabilityPage from '@/pages/RoomAvailabilityPage'
import AdminUsersPage from '@/pages/AdminUsersPage'
import UnauthorizedPage from '@/pages/UnauthorizedPage'
import AuthenticatedLayout from '@/components/AuthenticatedLayout'
import { useAuth } from '@/context/AuthContext'

const getDefaultRedirect = (roles: string[] | undefined): string => {
  if (roles?.includes('ROLE_ADMIN') || roles?.includes('ROLE_SUPER_ADMIN')) return '/admin'
  if (roles?.includes('ROLE_TTO')) return '/tto'
  if (roles?.includes('ROLE_DEPT_COORD') || roles?.includes('ROLE_EXAM_CONTROLLER')) return '/exam-ctrl'
  if (roles?.includes('ROLE_TEACHER')) return '/teacher'
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
        <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/auth/reset-password" element={<ResetPasswordPage />} />
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
          path="/tto/upload"
          element={
            <RequireRole allowedRoles={['ROLE_TTO', 'ROLE_ADMIN']}>
              {withLayout(<UploadTimetablePage />)}
            </RequireRole>
          }
        />
        <Route
          path="/tto/substitute"
          element={
            <RequireRole allowedRoles={['ROLE_TTO']}>
              {withLayout(<SubstitutePage />)}
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
          path="/admin/substitute"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']}>
              {withLayout(<SubstitutePage />)}
            </RequireRole>
          }
        />

        <Route
          path="/create-exam"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_EXAM_CONTROLLER', 'ROLE_DEPT_COORD']}>
              {withLayout(<CreateExamPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/exam-ctrl"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_EXAM_CONTROLLER', 'ROLE_DEPT_COORD']}>
              {withLayout(<ExamCtrlPage />)}
            </RequireRole>
          }
        />
        <Route
          path="/exam-ctrl/:examId"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_EXAM_CONTROLLER', 'ROLE_DEPT_COORD']}>
              {withLayout(<SeatingDashboard />)}
            </RequireRole>
          }
        />
        <Route
          path="/dept-coord/rooms"
          element={
            <RequireRole allowedRoles={['ROLE_DEPT_COORD']}>
              {withLayout(<RoomAvailabilityPage userRole="DEPT_COORD" />)}
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
        <Route
          path="/admin/users"
          element={
            <RequireRole allowedRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']}>
              {withLayout(<AdminUsersPage />)}
            </RequireRole>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
