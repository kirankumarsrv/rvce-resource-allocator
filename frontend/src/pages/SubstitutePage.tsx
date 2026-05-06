import { useState } from 'react'
import { substituteTeacher } from '../services/timetableService'
import { SubstituteRequest, SubstitutionResultDto } from '../types/timetable'

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

  const handleChange = (field: keyof SubstituteRequest, value: string) => {
    setFormState((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async () => {
    setError(null)
    setResult(null)

    if (!formState.originalTeacherId.trim() || !formState.replacementTeacherId.trim()) {
      setError('Please provide both teacher IDs.')
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
    } catch (submitError: any) {
      setError(submitError?.message || 'Substitution request failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Teacher Substitution</h1>
      <div className="bg-white rounded-xl shadow p-6 max-w-4xl">
        <p className="text-gray-600 mb-6">
          Reassign teaching slots from one teacher to another. This operation is available to TTO and Admin users.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Original Teacher ID</label>
            <input
              type="text"
              value={formState.originalTeacherId}
              onChange={(e) => handleChange('originalTeacherId', e.target.value)}
              placeholder="UUID of teacher to replace"
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Replacement Teacher ID</label>
            <input
              type="text"
              value={formState.replacementTeacherId}
              onChange={(e) => handleChange('replacementTeacherId', e.target.value)}
              placeholder="UUID of new teacher"
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Start Date</label>
            <input
              type="date"
              value={formState.startDate}
              onChange={(e) => handleChange('startDate', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">End Date</label>
            <input
              type="date"
              value={formState.endDate}
              onChange={(e) => handleChange('endDate', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">Substitution Scope</label>
            <select
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
            disabled={isSubmitting}
            onClick={handleSubmit}
            className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-white transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Submitting…' : 'Submit Substitution'}
          </button>
          <p className="text-sm text-gray-500 max-w-2xl">
            The substitution request will reassign matching slots and report any conflicts.
          </p>
        </div>

        {result && (
          <div className="mt-6 rounded-xl bg-green-50 border border-green-200 p-5">
            <h2 className="text-lg font-semibold text-green-700">Substitution Summary</h2>
            <div className="mt-3 text-sm text-slate-700">
              <p>Reassigned slots: {result.reassignedCount}</p>
              <p>Clashes detected: {result.clashes.length}</p>
              {result.clashes.length > 0 && (
                <ul className="mt-3 list-disc list-inside space-y-1">
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
