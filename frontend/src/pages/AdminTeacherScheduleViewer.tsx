import { useEffect, useState } from 'react'
import { TeacherScheduleDisplay } from '@/components/TeacherScheduleDisplay'
import { authenticatedFetch } from '@/services/authService'

interface TeacherOption {
  id: string
  text: string
}

export default function AdminTeacherScheduleViewer() {
  const [teachers, setTeachers] = useState<TeacherOption[]>([])
  const [selectedTeacher, setSelectedTeacher] = useState('')
  const [showSchedule, setShowSchedule] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchTeachers()
  }, [])

  const fetchTeachers = async () => {
    try {
      setLoading(true)
      setError('')

      const response = await authenticatedFetch(
        '/api/timetable/teachers'
      )

      if (!response.ok) {
        throw new Error(`Failed to load teachers (${response.status})`)
      }

      const data = await response.json()

      // Handle different backend response formats
      const mappedTeachers = (data || []).map((teacher: any) => ({
        id:
          teacher.id ??
          teacher.userId ??
          teacher.teacherId ??
          '',

        text:
          teacher.text ??
          teacher.name ??
          teacher.fullName ??
          teacher.displayName ??
          teacher.email ??
          'Unknown Teacher',
      }))

      setTeachers(mappedTeachers)
    } catch (err) {
      console.error(err)
      setError('Unable to load teachers')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">
          Teacher Schedule Viewer
        </h1>

        <p className="mt-2 text-slate-600">
          View timetable of any faculty member.
        </p>
      </div>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">
          Select Teacher
        </h2>

        {loading ? (
          <div className="text-slate-500">
            Loading teachers...
          </div>
        ) : error ? (
          <div className="text-red-600">
            {error}
          </div>
        ) : (
          <>
            <select
              value={selectedTeacher}
              onChange={(e) => {
                setSelectedTeacher(e.target.value)
                setShowSchedule(false)
              }}
              className="w-full px-4 py-2 border rounded-lg"
            >
              <option value="">
                Select Teacher
              </option>

              {teachers.map((teacher) => (
                <option
                  key={teacher.id}
                  value={teacher.id}
                >
                  {teacher.text}
                </option>
              ))}
            </select>

            <button
              onClick={() => setShowSchedule(true)}
              disabled={!selectedTeacher}
              className="mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400"
            >
              View Schedule
            </button>
          </>
        )}
      </div>

      {showSchedule && selectedTeacher && (
        <div className="bg-white rounded-lg shadow p-6">
          <TeacherScheduleDisplay
            teacherId={selectedTeacher}
          />
        </div>
      )}
    </div>
  )
}