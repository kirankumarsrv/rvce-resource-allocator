/**
 * BenchCell Component
 * Renders a single bench (2-seater or 3-seater) with visual seats
 * Handles click-to-place and hover interactions
 */

import { useMemo } from 'react'
import type { CSSProperties } from 'react'
import type { BenchCellDto, SeatDto, UnassignedStudentDto } from '@/types/exam'
import { useDashboardStore } from '@/store/seatingStore'

interface BenchCellProps {
  bench: BenchCellDto
  selectedStudent: UnassignedStudentDto | null
  onSeatClick: (seat: SeatDto, student: UnassignedStudentDto | null) => void
  pendingAssignments: Map<string, SeatDto>
  style?: CSSProperties
}

const branchColorMap: Record<string, string> = {
  CSE: 'bg-blue-100 border-blue-300',
  ISE: 'bg-green-100 border-green-300',
  ECE: 'bg-yellow-100 border-yellow-300',
  MECH: 'bg-red-100 border-red-300',
  CIVIL: 'bg-purple-100 border-purple-300',
  EEE: 'bg-orange-100 border-orange-300',
}

export const BenchCell = ({
  bench,
  selectedStudent,
  onSeatClick,
  pendingAssignments,
  style,
}: BenchCellProps) => {
  const { hoveredSeatId, setHoveredSeatId } = useDashboardStore()

  const seats = useMemo(() => {
    return (bench.seats ?? []).map((seat) => {
      const pending = pendingAssignments.get(seat.seatId)
      const assigned = pending || seat
      return { seat, assigned }
    })
  }, [bench.seats, pendingAssignments])

  if (bench.excluded) {
    return (
      <div
        style={style}
        className="flex flex-col items-center justify-center h-full min-h-[140px] w-full bg-gray-300 border-2 border-gray-500 rounded p-3"
      >
        <div className="text-xs font-bold text-gray-600">EXCLUDED</div>
        <div className="text-sm font-semibold text-gray-800 mt-1">{bench.benchNumber}</div>
      </div>
    )
  }

  return (
    <div
      style={style}
      className="flex h-full min-h-[140px] w-full flex-col justify-between rounded-lg border-2 border-gray-300 bg-white p-3 shadow-sm transition hover:border-blue-400 hover:bg-blue-50"
      onMouseEnter={() => setHoveredSeatId(bench.benchNumber)}
      onMouseLeave={() => setHoveredSeatId(null)}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="text-sm font-semibold text-gray-800">{bench.benchNumber}</div>
        <div className="text-xs text-gray-500">
          {bench.occupancyCount}/{bench.benchType}
        </div>
      </div>

      <div className={`grid gap-1 ${bench.benchType === 3 ? 'grid-cols-3' : 'grid-cols-2'}`}>
        {seats.map(({ seat, assigned }) => {
          const branchColor = assigned.branchCode
            ? branchColorMap[assigned.branchCode] || 'bg-gray-100 border-gray-300'
            : 'bg-gray-100 border-gray-300'

          const isHovered = hoveredSeatId === seat.seatId
          const isSelected = selectedStudent && selectedStudent.usn === assigned.usn

          return (
            <button
              type="button"
              key={seat.seatId}
              className={
                `w-full rounded-lg border-2 px-2 py-2 text-left text-[10px] transition ${branchColor} ${
                  isHovered ? 'ring-2 ring-blue-500' : ''
                } ${isSelected ? 'ring-2 ring-green-500 font-bold' : ''}`
              }
              onClick={() => onSeatClick(assigned, selectedStudent)}
              onMouseEnter={() => setHoveredSeatId(seat.seatId)}
              onMouseLeave={() => setHoveredSeatId(null)}
              title={
                assigned.studentName
                  ? selectedStudent
                    ? `Replace ${assigned.studentName} with selected student`
                    : `Unassign ${assigned.studentName}`
                  : 'Empty - Click to place'
              }
            >
              <div className="flex items-center justify-between gap-1">
                <span className="font-semibold text-[11px]">{seat.benchSeatIndex + 1}</span>
                <span className="text-[10px] text-gray-600 truncate">
                  {assigned.branchCode || 'Empty'}
                </span>
              </div>
              <div className="truncate text-xs font-semibold">
                {assigned.studentName ? assigned.studentName.split(' ')[0] : 'Empty'}
              </div>
              <div className="text-[10px] text-gray-600 truncate">
                {assigned.usn || ''}
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}
