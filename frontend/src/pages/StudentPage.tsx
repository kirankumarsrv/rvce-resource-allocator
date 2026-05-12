import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'

import { getStudentPublishedExams } from '@/services/examService'
import type { StudentPublishedExamDto } from '@/types/exam'

const formatDate = (value: string) => {
  try {
    return new Date(value).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return value
  }
}

const formatTime = (value: string) => {
  return value?.slice(0, 5) ?? value
}

const StudentPage = () => {
  const { data, isLoading, isError, error } = useQuery<StudentPublishedExamDto[]>({
    queryKey: ['student-published-exams'],
    queryFn: getStudentPublishedExams,
    refetchInterval: 20000,
  })

  const publishedExams = useMemo(() => data ?? [], [data])

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-5xl space-y-4">
        <div>
          <h1 data-test-id="student-dashboard-heading" className="text-3xl font-bold text-slate-900">Student Dashboard</h1>
          <p className="text-slate-600">Published exam seating assignments will appear here.</p>
        </div>

        {isLoading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-600">Loading published exams...</div>
        ) : null}

        {isError ? (
          <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
            {(error as Error)?.message ?? 'Unable to load published exams'}
          </div>
        ) : null}

        {!isLoading && !isError && publishedExams.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-700">
            No published seating is available yet.
          </div>
        ) : null}

        <div className="grid gap-4 md:grid-cols-2">
          {publishedExams.map((exam) => (
            <article key={exam.examId} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-900">{exam.examName}</h2>
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                  {exam.status}
                </span>
              </div>

              <div className="space-y-1 text-sm text-slate-700">
                <p>
                  <span className="font-medium">Subject:</span> {exam.subjectCode} - {exam.subjectName}
                </p>
                <p>
                  <span className="font-medium">Exam:</span> {formatDate(exam.examDate)} | {formatTime(exam.startTime)} - {formatTime(exam.endTime)}
                </p>
                <p>
                  <span className="font-medium">Hall:</span> {exam.hallName ?? 'Pending assignment'}
                </p>
                <p>
                  <span className="font-medium">Bench:</span> {exam.benchNumber ?? 'Pending assignment'}
                </p>
              </div>
            </article>
          ))}
        </div>
      </div>
    </div>
  )
}

export default StudentPage
