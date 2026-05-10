import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Loader, AlertCircle, Plus, Calendar, Users, Clock, Badge } from 'lucide-react'

import { listExamSessions } from '@/services/examService'
import type { ExamSessionDto } from '@/types/exam'

const ExamCtrlPage = () => {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const pageSize = 10

  const { data: sessionsData, isLoading, error } = useQuery({
    queryKey: ['exam-sessions', page],
    queryFn: () => listExamSessions(page, pageSize),
  })

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-800 border-gray-300',
      CONFIGURED: 'bg-blue-100 text-blue-800 border-blue-300',
      GENERATED: 'bg-purple-100 text-purple-800 border-purple-300',
      PUBLISHED: 'bg-green-100 text-green-800 border-green-300',
      COMPLETED: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      CANCELLED: 'bg-red-100 text-red-800 border-red-300',
    }
    return colors[status] || 'bg-gray-100 text-gray-800'
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-8">
        <Loader className="mr-2 animate-spin" />
        Loading exams...
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center p-8 text-red-600">
        <AlertCircle className="mr-2" />
        Failed to load exams: {(error as Error).message}
      </div>
    )
  }

  const exams = sessionsData?.content || []
  const totalPages = sessionsData?.totalPages || 0

  return (
    <div className="flex flex-col gap-6 p-6 bg-gray-50 min-h-screen">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Exam Control Portal</h1>
          <p className="text-gray-600 mt-1">Manage exam seating arrangements</p>
        </div>
        <button
          onClick={() => navigate('/create-exam')}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 transition"
        >
          <Plus size={20} />
          Create Exam
        </button>
      </div>

      {/* Exams List */}
      {exams.length === 0 ? (
        <div className="rounded-lg border border-gray-300 bg-white p-8 text-center">
          <AlertCircle className="mx-auto mb-2 text-gray-400" size={32} />
          <p className="text-gray-600">No exams found. Create a new exam to get started.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {exams.map((exam: ExamSessionDto) => (
            <div
              key={exam.examId}
              className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm hover:shadow-md transition cursor-pointer"
              onClick={() => navigate(`/exam-ctrl/${exam.examId}`)}
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h2 className="text-xl font-bold text-gray-800">{exam.name}</h2>
                    <Badge
                      className={`border px-3 py-1 text-sm font-semibold rounded-full ${getStatusColor(exam.status)}`}
                    >
                      {exam.status}
                    </Badge>
                  </div>
                  <p className="text-sm text-gray-600 mb-3">
                    <span className="font-semibold">{exam.subjectCode}</span> • {exam.departmentName} • Semester{' '}
                    {exam.semester}
                  </p>

                  <div className="grid grid-cols-4 gap-4 text-sm">
                    <div className="flex items-center gap-2 text-gray-700">
                      <Calendar size={16} className="text-gray-400" />
                      <span>{new Date(exam.examDate).toLocaleDateString()}</span>
                    </div>
                    <div className="flex items-center gap-2 text-gray-700">
                      <Clock size={16} className="text-gray-400" />
                      <span>
                        {exam.startTime} - {exam.endTime}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-gray-700">
                      <Users size={16} className="text-gray-400" />
                      <span>{exam.studentCount || 0} students</span>
                    </div>
                    <div className="text-right text-gray-600">
                      <span className="text-xs">Updated {new Date(exam.updatedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    navigate(`/exam-ctrl/${exam.examId}`)
                  }}
                  className="ml-4 rounded-lg bg-green-600 px-4 py-2 font-semibold text-white hover:bg-green-700 transition whitespace-nowrap"
                >
                  Manage Seating
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 mt-4">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          <div className="flex items-center gap-1">
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              const pageNum = Math.max(0, Math.min(page - 2 + i, totalPages - 1))
              return (
                <button
                  key={pageNum}
                  onClick={() => setPage(pageNum)}
                  className={`px-3 py-2 rounded-lg border ${
                    page === pageNum
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'border-gray-300 bg-white hover:bg-gray-50'
                  }`}
                >
                  {pageNum + 1}
                </button>
              )
            })}
          </div>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

export default ExamCtrlPage
