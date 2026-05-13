import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getTeachers, substituteTeacher } from '../services/timetableService'
import { SimpleDto, SubstituteRequest, SubstitutionResultDto } from '../types/timetable'

const SubstitutePage = () => {
  const [formState, setFormState] = useState<SubstituteRequest>({
    originalTeacherId: '',
    replacementTeacherId: '',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
    scope: 'ONE_DAY',
  })
  const [result, setResult] = useState<SubstitutionResultDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const teachersQuery = useQuery<SimpleDto[]>({
    queryKey: ['teachers'],
    queryFn: getTeachers,
  })

  const handleChange = (field: keyof SubstituteRequest, value: string) => {
    setFormState((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async () => {
    setError(null)
    setResult(null)

    if (!formState.originalTeacherId.trim() || !formState.replacementTeacherId.trim()) {
      setError('Please select both teachers.')
      return
    }

    if (formState.originalTeacherId === formState.replacementTeacherId) {
      setError('Replacement teacher must be different from the original teacher.')
      return
    }

    if (formState.startDate > formState.endDate) {
      setError('End date must be on or after the start date.')
      return
    }

    setIsSubmitting(true)
    try {
      const response = await substituteTeacher(formState)
      setResult(response)
    } catch (submitError: unknown) {
      const errorMessage = submitError instanceof Error ? submitError.message : 'Substitution request failed'
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div data-test-id="substitute-page" className="p-6">
      <h1 className="text-3xl font-bold mb-6">Teacher Substitution</h1>
      <div className="bg-white rounded-xl shadow p-6 max-w-4xl">
        <p className="text-gray-600 mb-6">
          Reassign teaching slots from one teacher to another. This operation is available to TTO and Admin users.
        </p>

        {teachersQuery.isError && (
          <div className="mb-6 rounded-lg bg-red-50 border border-red-200 p-4 text-red-700">
            Failed to load teacher list. You can still submit if you know the teacher IDs.
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Original Teacher</label>
            <select
              data-test-id="substitute-original-teacher"
              value={formState.originalTeacherId}
              onChange={(e) => handleChange('originalTeacherId', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              disabled={teachersQuery.isLoading}
            >
              <option value="">Select the teacher to replace</option>
              {teachersQuery.data?.map((teacher) => (
                <option key={teacher.id} value={teacher.id}>
                  {teacher.text}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Replacement Teacher</label>
            <select
              data-test-id="substitute-replacement-teacher"
              value={formState.replacementTeacherId}
              onChange={(e) => handleChange('replacementTeacherId', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              disabled={teachersQuery.isLoading}
            >
              <option value="">Select the replacement teacher</option>
              {teachersQuery.data?.map((teacher) => (
                <option key={teacher.id} value={teacher.id}>
                  {teacher.text}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Start Date</label>
            <input
              data-test-id="substitute-start-date"
              type="date"
              value={formState.startDate}
              onChange={(e) => handleChange('startDate', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">End Date</label>
            <input
              data-test-id="substitute-end-date"
              type="date"
              value={formState.endDate}
              onChange={(e) => handleChange('endDate', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">Substitution Scope</label>
            <select
              data-test-id="substitute-scope"
              value={formState.scope}
              onChange={(e) => handleChange('scope', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              <option value="ONE_DAY">One Day</option>
              <option value="SEMESTER">Semester</option>
            </select>
          </div>
        </div>

        {error && (
          <div className="mt-6 rounded-lg bg-red-50 border border-red-200 p-4 text-red-700">
            {error}
          </div>
        )}

        <div className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-center">
          <button
            data-test-id="substitute-submit"
            disabled={isSubmitting || teachersQuery.isLoading}
            onClick={handleSubmit}
            className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-white transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Submitting…' : teachersQuery.isLoading ? 'Loading teachers…' : 'Submit Substitution'}
          </button>
          <p className="text-sm text-gray-500 max-w-2xl">
            The substitution request will reassign matching slots and report any conflicts.
          </p>
        </div>

        {result && (
          <div data-test-id="substitution-summary" className="mt-6 rounded-xl bg-green-50 border border-green-200 p-5">
            <h2 className="text-lg font-semibold text-green-700">Substitution Summary</h2>
            <div className="mt-3 text-sm text-slate-700">
              <p>Reassigned slots: {result.reassignedCount}</p>
              <p>Clashes detected: {result.clashes.length}</p>
              {result.clashes.length > 0 && (
                <ul data-test-id="substitution-clash-list" className="mt-3 list-disc list-inside space-y-1">
                  {result.clashes.map((clash, idx) => (
                    <li key={idx}>{clash}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default SubstitutePage
