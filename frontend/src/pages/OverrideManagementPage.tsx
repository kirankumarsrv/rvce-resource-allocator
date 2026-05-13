import { useCallback, useEffect, useState } from 'react'
import { createOverride, deleteOverride, getOverrides, getTeachers, getTeacherSchedule } from '../services/timetableService'
import { OverrideDto, OverrideRequest, SimpleDto, TeacherScheduleDto } from '../types/timetable'

const OverrideManagementPage = () => {
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0])
  const [roomId, setRoomId] = useState('')
  const [overrides, setOverrides] = useState<OverrideDto[]>([])
  const [fetching, setFetching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  // Slot discovery state
  const [teachers, setTeachers] = useState<SimpleDto[]>([])
  const [selectedTeacherId, setSelectedTeacherId] = useState('')
  const [slots, setSlots] = useState<TeacherScheduleDto[]>([])
  const [loadingSlots, setLoadingSlots] = useState(false)

  const [formState, setFormState] = useState<OverrideRequest>({
    slotId: 0,
    date: new Date().toISOString().split('T')[0],
    status: 'CANCELLED',
    reason: '',
  })
  const [isSaving, setIsSaving] = useState(false)

  const fetchTeachers = useCallback(async () => {
    try {
      const data = await getTeachers()
      setTeachers(data)
    } catch {
      setTeachers([])
    }
  }, [])

  useEffect(() => {
    fetchTeachers()
  }, [fetchTeachers])

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

  const fetchTeacherSlots = useCallback(async () => {
    if (!selectedTeacherId) {
      setSlots([])
      return
    }
    setLoadingSlots(true)
    try {
      const dateObj = new Date(formState.date)
      const dayOfWeek = dateObj.getDay()
      const data = await getTeacherSchedule(selectedTeacherId, dayOfWeek)
      setSlots(data)
    } catch {
      setSlots([])
    } finally {
      setLoadingSlots(false)
    }
  }, [selectedTeacherId, formState.date])

  useEffect(() => {
    fetchOverrides()
  }, [fetchOverrides])

  useEffect(() => {
    fetchTeacherSlots()
  }, [fetchTeacherSlots])

  const handleChange = (field: keyof OverrideRequest, value: string | number) => {
    setFormState((prev: OverrideRequest) => ({ ...prev, [field]: value }))
  }

  const handleSelectSlot = (slot: TeacherScheduleDto) => {
    setFormState((prev: OverrideRequest) => ({ ...prev, slotId: slot.slotId }))
    setMessage(`Selected slot ${slot.slotId} (${slot.subject} in ${slot.roomName})`)
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
      setFormState((prev: OverrideRequest) => ({ ...prev, slotId: 0, reason: '' }))
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
      <div className="grid gap-8 lg:grid-cols-3">
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
          <h2 className="text-xl font-semibold mb-4">Discover Slots</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Teacher</label>
              <select
                value={selectedTeacherId}
                onChange={(e) => setSelectedTeacherId(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              >
                <option value="">Select a teacher…</option>
                {teachers.map((teacher) => (
                  <option key={teacher.id} value={teacher.id}>
                    {teacher.text}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Date (for day of week)</label>
              <input
                type="date"
                value={formState.date}
                onChange={(e) => handleChange('date', e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Available Slots</label>
              {loadingSlots && <p className="text-gray-600">Loading slots…</p>}
              {slots.length === 0 && !loadingSlots && (
                <p className="text-gray-600">No slots found for this teacher on the selected day.</p>
              )}
              <div className="space-y-2">
                {slots.map((slot) => (
                  <button
                    key={slot.slotId}
                    onClick={() => handleSelectSlot(slot)}
                    className={`w-full text-left p-3 rounded-lg border transition ${
                      formState.slotId === slot.slotId
                        ? 'bg-blue-50 border-blue-500'
                        : 'bg-gray-50 border-gray-300 hover:bg-gray-100'
                    }`}
                  >
                    <div className="font-semibold text-sm">{slot.subject}</div>
                    <div className="text-xs text-gray-600">{slot.roomName} ({slot.roomBuilding}) • {slot.dayOfWeek} • {slot.startTime} - {slot.endTime}</div>
                  </button>
                ))}
              </div>
            </div>
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
