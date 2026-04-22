import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import AdminPage from '@/pages/AdminPage'
import ExamCtrlPage from '@/pages/ExamCtrlPage'
import StudentPage from '@/pages/StudentPage'
import TeacherPage from '@/pages/TeacherPage'
import TTOPage from '@/pages/TTOPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/student" replace />} />

        <Route path="/student" element={<StudentPage />} />
        <Route path="/teacher" element={<TeacherPage />} />
        <Route path="/tto" element={<TTOPage />} />
        <Route path="/exam-ctrl" element={<ExamCtrlPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
