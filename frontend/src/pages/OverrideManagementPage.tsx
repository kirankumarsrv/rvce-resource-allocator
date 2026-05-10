import { useCallback, useEffect, useState } from 'react'
import { createOverride, deleteOverride, getOverrides } from '../services/timetableService'
import { OverrideDto, OverrideRequest } from '../types/timetable'

const OverrideManagementPage = () => {
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0])
  const [roomId, setRoomId] = useState('')
  const [overrides, setOverrides] = useState<OverrideDto[]>([])
  const [fetching, setFetching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [formState, setFormState] = useState<OverrideRequest>({
    slotId: 0,
    date: new Date().toISOString().split('T')[0],
    status: 'CANCELLED',
    reason: '',
  })
  const [isSaving, setIsSaving] = useState(false)

  const fetchOverrides = useCallback(async () => {
    setFetching(true)
    setError(null)
    try {
      const data = await getOverrides(date, roomId || undefined)
      setOverrides(data)
      setMessage(`Found ${data.length} override${data.length === 1 ? '' : 's'} for ${date}`)
    } catch (fetchError: unknown) {
      const errorMessage = fetchError instanceof Error ? fetchError.message : 'Unable to fetch overrides'
      setError(errorMessage)
      setOverrides([])
    } finally {
      setFetching(false)
    }
  }, [date, roomId])

  useEffect(() => {
    fetchOverrides()
  }, [fetchOverrides])

  const handleChange = (field: keyof OverrideRequest, value: string | number) => {
    setFormState((prev) => ({ ...prev, [field]: value }))
  }

  const handleCreateOverride = async () => {
    setError(null)
    setMessage(null)

    if (!formState.slotId || !formState.date) {
      setError('Slot ID and date are required.')
      return
    }

    setIsSaving(true)
    try {
      await createOverride(formState)
      setMessage('Override saved successfully.')
      setFormState((prev) => ({ ...prev, slotId: 0, reason: '' }))
      await fetchOverrides()
    } catch (saveError: unknown) {
      const errorMessage = saveError instanceof Error ? saveError.message : 'Unable to save override'
      setError(errorMessage)
    } finally {
      setIsSaving(false)
    }
  }

  const handleDelete = async (overrideId: string) => {
    setError(null)
    setMessage(null)

    try {
      await deleteOverride(overrideId)
      setMessage('Override deleted successfully.')
      await fetchOverrides()
    } catch (deleteError: unknown) {
      const errorMessage = deleteError instanceof Error ? deleteError.message : 'Unable to delete override'
      setError(errorMessage)
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Override Management</h1>
      <div className="grid gap-8 lg:grid-cols-2">
        <section className="bg-white rounded-xl shadow p-6">
          <h2 className="text-xl font-semibold mb-4">Search Overrides</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Room ID (optional)</label>
              <input
                type="text"
                value={roomId}
                onChange={(e) => setRoomId(e.target.value)}
                placeholder="Filter by room ID"
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
            <button
              onClick={fetchOverrides}
              disabled={fetching}
              className="inline-flex items-center rounded-lg bg-blue-600 px-5 py-3 text-white transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {fetching ? 'Refreshing…' : 'Refresh Overrides'}
            </button>
          </div>

          {message && (
            <div className="mt-6 rounded-lg bg-green-50 border border-green-200 p-4 text-green-700">
              {message}
            </div>
          )}

          {error && (
            <div className="mt-6 rounded-lg bg-red-50 border border-red-200 p-4 text-red-700">
              {error}
            </div>
          )}

          <div className="mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-gray-600 uppercase tracking-wide">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Slot ID</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Reason</th>
                  <th className="px-4 py-3">Created By</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {overrides.map((overrideRecord) => (
                  <tr key={overrideRecord.id}>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.id}</td>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.slotId}</td>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.date}</td>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.status}</td>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.reason || '-'}</td>
                    <td className="px-4 py-3 text-slate-700">{overrideRecord.createdBy}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleDelete(String(overrideRecord.id))}
                        className="rounded-lg bg-red-600 px-3 py-2 text-sm text-white transition hover:bg-red-700"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {overrides.length === 0 && !fetching && (
                  <tr>
                    <td colSpan={7} className="px-4 py-6 text-center text-gray-500">
                      No overrides found for the selected date.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="bg-white rounded-xl shadow p-6">
          <h2 className="text-xl font-semibold mb-4">Create Override</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Slot ID</label>
              <input
                type="number"
                value={formState.slotId || ''}
                onChange={(e) => handleChange('slotId', Number(e.target.value))}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Date</label>
              <input
                type="date"
                value={formState.date}
                onChange={(e) => handleChange('date', e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Status</label>
              <select
                value={formState.status}
                onChange={(e) => handleChange('status', e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              >
                <option value="CANCELLED">CANCELLED</option>
                <option value="CLAIMED">CLAIMED</option>
                <option value="EXTRA_CLASS">EXTRA_CLASS</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Reason</label>
              <textarea
                value={formState.reason}
                onChange={(e) => handleChange('reason', e.target.value)}
                rows={4}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                placeholder="Optional reason for the override"
              />
            </div>
            <button
              onClick={handleCreateOverride}
              disabled={isSaving}
              className="inline-flex items-center rounded-lg bg-blue-600 px-6 py-3 text-white transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {isSaving ? 'Saving…' : 'Save Override'}
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}

export default OverrideManagementPage
