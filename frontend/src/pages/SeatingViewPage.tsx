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

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Seating Arrangement</h1>
          <p className="text-slate-600">Exam ID: {seatingState.examId}</p>
        </div>

        <div className="space-y-6">
          {seatingState.hallGrids.map((hallGrid) => (
            <div key={hallGrid.hallId} className="rounded-xl border border-slate-200 bg-white p-6">
              <div className="mb-4">
                <h2 className="text-xl font-semibold text-slate-900">
                  {hallGrid.roomDisplayName || hallGrid.roomName}
                </h2>
                <p className="text-sm text-slate-600">
                  {hallGrid.benchRows} rows × {hallGrid.benchCols} columns
                </p>
              </div>

              <div className="grid gap-3" style={{ gridTemplateColumns: `repeat(${hallGrid.benchCols}, minmax(0, 1fr))` }}>
                {hallGrid.grid.flatMap((row, rowIndex) =>
                  row.map((cell, colIndex) => (
                    <div
                      key={`${hallGrid.hallId}-${rowIndex}-${colIndex}`}
                      className={`rounded-2xl border p-3 text-xs font-medium ${
                        cell.excluded
                          ? 'border-rose-200 bg-rose-50 text-rose-700'
                          : 'border-slate-200 bg-slate-50 text-slate-800'
                      }`}
                    >
                      <div className="mb-2 flex items-center justify-between">
                        <span className="text-slate-700">{cell.label}</span>
                        <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-semibold text-slate-600">
                          {cell.occupiedCount}/{cell.seatCapacity}
                        </span>
                      </div>
                      <div className="space-y-1">
                        {cell.seats.map((seat) => (
                          <div
                            key={seat.seatId}
                            className={`rounded-xl px-2 py-1 ${seat.usn ? 'bg-blue-50 text-blue-900' : 'bg-white text-slate-500'}`}
                          >
                            {seat.usn || 'empty'}
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default SeatingViewPage