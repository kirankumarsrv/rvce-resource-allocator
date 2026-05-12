import { FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { createExamSession } from '@/services/examService'

const CreateExamPage = () => {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [subjectCode, setSubjectCode] = useState('')
  const [subjectName, setSubjectName] = useState('')
  const [section, setSection] = useState('')
  const [semester, setSemester] = useState('5')
  const [examDate, setExamDate] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    document.title = 'Create Exam | RVCE Resource Allocator'
  }, [])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const created = await createExamSession({
        name: name.trim(),
        subjectCode: subjectCode.trim(),
        subjectName: subjectName.trim(),
        section: section.trim() || null,
        semester: Number(semester),
        examDate,
        startTime,
        endTime,
      })

      navigate(`/exam-ctrl/${created.examId}`)
    } catch (submitError: unknown) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to create exam session')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10">
      <div className="mx-auto w-full max-w-3xl rounded-3xl border border-slate-200 bg-white p-8 shadow-lg shadow-slate-200/50">
        <h1 data-test-id="create-exam-heading" className="text-3xl font-semibold text-slate-900">Create Exam Session</h1>
        <p className="mt-2 text-sm text-slate-600">Set up the exam metadata before uploading students and configuring seating.</p>

        <form onSubmit={handleSubmit} className="mt-8 grid gap-5 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700">Exam name</label>
            <input
              data-test-id="exam-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="Dec 2026 CIE-1 - 5th Sem CSE"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">Subject code</label>
            <input
              data-test-id="exam-subject-code"
              value={subjectCode}
              onChange={(event) => setSubjectCode(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="21CS51"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">Semester</label>
            <input
              data-test-id="exam-semester"
              type="number"
              min={1}
              max={8}
              value={semester}
              onChange={(event) => setSemester(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              required
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-700">Subject name</label>
            <input
              data-test-id="exam-subject-name"
              value={subjectName}
              onChange={(event) => setSubjectName(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="Design and Analysis of Algorithms"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">Section</label>
            <input
              data-test-id="exam-section"
              value={section}
              onChange={(event) => setSection(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              placeholder="A"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">Exam date</label>
            <input
              data-test-id="exam-date"
              type="date"
              value={examDate}
              onChange={(event) => setExamDate(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">Start time</label>
            <input
              data-test-id="exam-start-time"
              type="time"
              value={startTime}
              onChange={(event) => setStartTime(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">End time</label>
            <input
              data-test-id="exam-end-time"
              type="time"
              value={endTime}
              onChange={(event) => setEndTime(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
              required
            />
          </div>

          {error ? (
            <div data-test-id="create-exam-error" className="md:col-span-2 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          <div className="md:col-span-2 flex justify-end gap-3 pt-2">
            <button
              type="button"
              data-test-id="create-exam-cancel"
              onClick={() => navigate('/exam-ctrl')}
              className="rounded-2xl border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              data-test-id="create-exam-submit"
              disabled={isSubmitting}
              className="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? 'Creating…' : 'Create session'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CreateExamPage