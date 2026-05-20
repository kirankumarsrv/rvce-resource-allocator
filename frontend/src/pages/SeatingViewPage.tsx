import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { viewSeating } from '@/services/examService'
import type { SeatingDashboardStateDto } from '@/types/exam'

const SeatingViewPage = () => {
  const { examId } = useParams<{ examId: string }>()

  const { data: seatingState, isLoading, isError, error } = useQuery<SeatingDashboardStateDto>({
    queryKey: ['exam-seating-view', examId],
    queryFn: () => viewSeating(examId!),
    enabled: !!examId,
  })

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 px-4 py-8">
        <div className="mx-auto max-w-5xl">
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-600">
            Loading seating arrangement...
          </div>
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="min-h-screen bg-slate-50 px-4 py-8">
        <div className="mx-auto max-w-5xl">
          <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
            {(error as Error)?.message ?? 'Unable to load seating arrangement'}
          </div>
        </div>
      </div>
    )
  }

  if (!seatingState) {
    return (
      <div className="min-h-screen bg-slate-50 px-4 py-8">
        <div className="mx-auto max-w-5xl">
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-700">
            No seating data available.
          </div>
        </div>
      </div>
    )
  }

  const hallsWithAssignments = seatingState.halls.map((hall) => {
    const assignments = seatingState.assignedSeats
      .filter((seat) => seat.hallId === hall.hallId && (seat.usn || seat.studentName))
      .sort((a, b) => {
        if (a.benchRow !== b.benchRow) return a.benchRow - b.benchRow
        if (a.benchCol !== b.benchCol) return a.benchCol - b.benchCol
        return a.benchSeatIndex - b.benchSeatIndex
      })

    return { hall, assignments }
  })

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Seating Assignment List</h1>
          <p className="text-slate-600">Exam ID: {seatingState.examId}</p>
        </div>

        <div className="space-y-6">
          {hallsWithAssignments.map(({ hall, assignments }) => (
            <div key={hall.hallId} className="rounded-xl border border-slate-200 bg-white p-6">
              <div className="mb-4 flex items-start justify-between gap-4 sm:items-center">
                <div>
                  <h2 className="text-xl font-semibold text-slate-900">
                    {hall.roomDisplayName || hall.roomName}
                  </h2>
                  <p className="text-sm text-slate-600">
                    {assignments.length}/{hall.totalCapacity} students assigned
                  </p>
                </div>
                <div className="text-sm text-slate-600">
                  {hall.invigilatorName ? `Invigilator: ${hall.invigilatorName}` : 'No invigilator assigned'}
                </div>
              </div>

              {assignments.length > 0 ? (
                <div className="space-y-3">
                  {assignments.map((seat) => (
                    <div
                      key={seat.seatId}
                      className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between"
                    >
                      <div>
                        <div className="text-sm font-semibold text-slate-900">
                          {seat.studentName || seat.usn || 'Unknown student'}
                        </div>
                        <div className="text-xs text-slate-600">
                          {seat.usn ?? 'No USN'} · {seat.branchCode ?? 'Unknown branch'}
                        </div>
                      </div>
                      <div className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                        Bench {seat.benchNumber} · Seat {seat.benchSeatIndex + 1}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-slate-600">
                  No student assignments are available for this hall yet.
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default SeatingViewPage