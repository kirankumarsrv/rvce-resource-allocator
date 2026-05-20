/**
 * AddHallModal Component
 * Form to add a new exam hall to the current exam session.
 * Updated: Invigilator is now required with teacher dropdown.
 */

import { useEffect, useState } from 'react'
import { X, Loader } from 'lucide-react'
import type { ExamHallConfigRequest } from '@/types/exam'
import type { RoomAvailabilityDto, SimpleDto } from '@/types/timetable'
import { listAvailableRooms } from '@/services/examService'
import { getTeachers } from '@/services/timetableService'

interface AddHallModalProps {
  examId: string
  isOpen: boolean
  onClose: () => void
  onSubmit: (config: ExamHallConfigRequest) => void
  isSubmitting: boolean
}

type AvailableRoom = RoomAvailabilityDto
export const AddHallModal = ({
  examId,
  isOpen,
  onClose,
  onSubmit,
  isSubmitting,
}: AddHallModalProps) => {
  const [rooms, setRooms] = useState<AvailableRoom[]>([])
  const [teachers, setTeachers] = useState<SimpleDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [roomLoadError, setRoomLoadError] = useState<string | null>(null)
  const [teacherLoadError, setTeacherLoadError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [roomId, setRoomId] = useState('')
  const [twoSeaterCount, setTwoSeaterCount] = useState(0)
  const [threeSeaterCount, setThreeSeaterCount] = useState(0)
  const [invigilatorId, setInvigilatorId] = useState('')

  useEffect(() => {
    if (!isOpen) return

    // Reset form state
    setRoomId('')
    setTwoSeaterCount(0)
    setThreeSeaterCount(0)
    setInvigilatorId('')
    setLoadError(null)
    setRoomLoadError(null)
    setTeacherLoadError(null)

    setIsLoading(true)

    const loadRooms = listAvailableRooms(examId)
      .then((availableRooms) => {
        const examHallRooms = availableRooms.filter((room) => room.roomType === 'EXAM_HALL')
        setRooms(examHallRooms)

        if (examHallRooms.length > 0) {
          setRoomId(examHallRooms[0].id)
          const firstRoom = examHallRooms[0]
          const capacity = firstRoom.capacity
          const threeSeaterCount = Math.min(2, Math.floor(capacity / 3))
          const remainingCapacity = capacity - (threeSeaterCount * 3)
          const twoSeaterCount = Math.floor(remainingCapacity / 2)

          setTwoSeaterCount(twoSeaterCount)
          setThreeSeaterCount(threeSeaterCount)
        }
      })
      .catch((err) => {
        setRooms([])
        setRoomLoadError(err instanceof Error ? err.message : 'Failed to load rooms')
      })

    const loadTeacherList = getTeachers()
      .then((teacherList) => {
        setTeachers(teacherList)
      })
      .catch((err) => {
        setTeachers([])
        setTeacherLoadError(err instanceof Error ? err.message : 'Failed to load teachers')
      })

    Promise.allSettled([loadRooms, loadTeacherList]).finally(() => {
      setIsLoading(false)
    })
  }, [isOpen])

  // Clear errors when inputs change
  useEffect(() => {
    if (loadError) {
      setLoadError(null)
    }
  }, [roomId, twoSeaterCount, threeSeaterCount, invigilatorId])

  const handleSubmit = () => {
    if (!roomId) {
      setLoadError('Please select a room.')
      return
    }

    if (!invigilatorId) {
      setLoadError('Please select an invigilator (required).')
      return
    }

    const totalBenches = twoSeaterCount + threeSeaterCount
    const totalCapacity = (twoSeaterCount * 2) + (threeSeaterCount * 3)

    if (totalBenches === 0) {
      setLoadError('Please specify at least one bench (2-seater or 3-seater).')
      return
    }

    if (totalCapacity === 0) {
      setLoadError('Total seating capacity cannot be zero.')
      return
    }

    // Find selected room to check capacity
    const selectedRoom = rooms.find(r => r.id === roomId)
    if (selectedRoom && totalCapacity > selectedRoom.capacity) {
      setLoadError(`Total capacity (${totalCapacity}) exceeds room capacity (${selectedRoom.capacity}).`)
      return
    }

    onSubmit({
      roomId,
      twoSeaterCount,
      threeSeaterCount,
      invigilatorId: invigilatorId.trim(),
    })
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Add New Hall</h2>
            <p className="text-sm text-gray-600">Select an exam hall, invigilator, and configure seating.</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700"
            aria-label="Close modal"
          >
            <X size={18} />
          </button>
        </div>

        <div className="mt-6 space-y-4">
          {isLoading ? (
            <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm text-gray-700">
              <Loader className="animate-spin" size={18} /> Loading rooms and teachers...
            </div>
          ) : (
            <>
              {(roomLoadError || teacherLoadError) && (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 space-y-1">
                  {roomLoadError ? <p>Rooms: {roomLoadError}</p> : null}
                  {teacherLoadError ? <p>Teachers: {teacherLoadError}</p> : null}
                </div>
              )}

              {loadError ? (
                <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                  {loadError}
                </div>
              ) : null}

              {rooms.length === 0 ? (
                <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-sm text-yellow-700">
                  No exam hall rooms are available for this exam time. Please ensure at least one room is configured as an exam hall and try again.
                </div>
              ) : (
                <div>
                  <label htmlFor="roomId" className="text-sm font-semibold text-gray-700">
                    Room
                  </label>
                  <select
                    id="roomId"
                    data-test-id="add-hall-room"
                    value={roomId}
                    onChange={(event) => {
                      const selectedRoomId = event.target.value
                      setRoomId(selectedRoomId)
                      
                      // Auto-fill reasonable bench counts based on room capacity
                      const selectedRoom = rooms.find((r) => r.id === selectedRoomId)
                      if (selectedRoom) {
                        const capacity = selectedRoom.capacity
                        const threeSeaterCount = Math.min(2, Math.floor(capacity / 3))
                        const remainingCapacity = capacity - (threeSeaterCount * 3)
                        const twoSeaterCount = Math.floor(remainingCapacity / 2)

                        setTwoSeaterCount(twoSeaterCount)
                        setThreeSeaterCount(threeSeaterCount)
                      }
                    }}
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900"
                  >
                    {rooms.map((room) => (
                      <option key={room.id} value={room.id}>
                        {room.name} • Block {room.building}, Floor {room.floor} • Capacity {room.capacity}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {teachers.length === 0 ? (
                <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-sm text-yellow-700">
                  No teachers are available. Please contact admin to add teachers.
                </div>
              ) : (
                <div>
                  <label htmlFor="invigilatorId" className="text-sm font-semibold text-gray-700">
                    Invigilator <span className="text-red-600">*</span>
                  </label>
                  <select
                    id="invigilatorId"
                    data-test-id="add-hall-invigilator"
                    value={invigilatorId}
                    onChange={(event) => setInvigilatorId(event.target.value)}
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900"
                  >
                    <option value="">-- Select an invigilator --</option>
                    {teachers.map((teacher) => (
                      <option key={teacher.id} value={teacher.id}>
                        {teacher.text}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="twoSeaterCount" className="text-sm font-semibold text-gray-700">
                    2-seater benches
                  </label>
                  <input
                    id="twoSeaterCount"
                    data-test-id="add-hall-two-seater"
                    type="number"
                    min={0}
                    value={twoSeaterCount}
                    onChange={(event) => setTwoSeaterCount(Number(event.target.value))}
                    className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="threeSeaterCount" className="text-sm font-semibold text-gray-700">
                    3-seater benches
                  </label>
                  <input
                    id="threeSeaterCount"
                    data-test-id="add-hall-three-seater"
                    type="number"
                    min={0}
                    value={threeSeaterCount}
                    onChange={(event) => setThreeSeaterCount(Number(event.target.value))}
                    className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              {/* Capacity Summary */}
              <div className="rounded-lg bg-blue-50 p-4 border border-blue-200">
                <h3 className="text-sm font-semibold text-blue-800 mb-2">Capacity Summary</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">2-seater benches:</span>
                    <span className="ml-2 font-semibold">{twoSeaterCount} × 2 = {twoSeaterCount * 2} seats</span>
                  </div>
                  <div>
                    <span className="text-gray-600">3-seater benches:</span>
                    <span className="ml-2 font-semibold">{threeSeaterCount} × 3 = {threeSeaterCount * 3} seats</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-gray-600">Total capacity:</span>
                    <span className="ml-2 font-bold text-blue-800">{(twoSeaterCount * 2) + (threeSeaterCount * 3)} seats</span>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            data-test-id="add-hall-cancel"
            onClick={onClose}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="button"
            data-test-id="add-hall-submit"
            onClick={handleSubmit}
            className="inline-flex items-center justify-center rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting || isLoading || rooms.length === 0}
          >
            {isSubmitting ? 'Adding...' : 'Add hall'}
          </button>
        </div>
      </div>
    </div>
  )
}
