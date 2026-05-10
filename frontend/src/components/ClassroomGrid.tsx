/**
 * ClassroomGrid Component
 * Renders the full 2D classroom layout with all benches
 * Handles seat placement interactions
 */

import { useMemo } from 'react'
import type {
  HallGridDto,
  HallGridCellDto,
  SeatDto,
  UnassignedStudentDto,
  ExamSeatDto,
} from '@/types/exam'
import { BenchCell } from './BenchCell'
import { useDashboardStore } from '@/store/seatingStore'

interface ClassroomGridProps {
  hallGrid: HallGridDto
  assignedSeats: ExamSeatDto[]
  selectedStudent: UnassignedStudentDto | null
  pendingAssignments: Map<string, SeatDto>
  removedAssignments: Set<string>
  onSeatClick: (seat: SeatDto, student: UnassignedStudentDto | null) => void
}

const buildSeatPlaceholders = (
  hallId: string,
  benchRow: number,
  benchCol: number,
  capacity: number,
  assignedByPosition: Map<string, SeatDto>,
  pendingByPosition: Map<string, SeatDto>
): SeatDto[] => {
  const seats: SeatDto[] = []
  for (let index = 0; index < capacity; index += 1) {
    const positionKey = `${benchRow}:${benchCol}:${index}`
    const assigned = pendingByPosition.get(positionKey) ?? assignedByPosition.get(positionKey)
    if (assigned) {
      seats.push(assigned)
      continue
    }

    seats.push({
      seatId: `placeholder-${hallId}-${benchRow}-${benchCol}-${index}`,
      examId: hallId,
      studentId: null,
      hallId,
      benchRow,
      benchCol,
      benchSeatIndex: index,
      benchNumber: `${String.fromCharCode(65 + benchRow - 1)}-${benchCol}`,
      usn: null,
      studentName: null,
      branchCode: null,
      isManualOverride: false,
    })
  }
  return seats
}

const normalizeBench = (
  cell: HallGridCellDto,
  hallId: string,
  assignedByPosition: Map<string, SeatDto>,
  pendingByPosition: Map<string, SeatDto>
) => {
  const seats = buildSeatPlaceholders(
    hallId,
    cell.row,
    cell.col,
    cell.seatCapacity,
    assignedByPosition,
    pendingByPosition
  )

  return {
    row: cell.row,
    col: cell.col,
    benchNumber: cell.label,
    benchType: cell.seatCapacity as 2 | 3,
    seats,
    occupancyCount: seats.filter((seat) => Boolean(seat.studentId)).length,
    excluded: cell.excluded,
  }
}

export const ClassroomGrid = ({
  hallGrid,
  assignedSeats,
  selectedStudent,
  pendingAssignments,
  removedAssignments,
  onSeatClick,
}: ClassroomGridProps) => {
  const { setHoveredSeatId } = useDashboardStore()

  const assignedByPosition = useMemo(() => {
    const map = new Map<string, SeatDto>()
    assignedSeats.forEach((seat) => {
      const key = `${seat.benchRow}:${seat.benchCol}:${seat.benchSeatIndex}`
      if (!removedAssignments.has(key)) {
        map.set(key, {
          ...seat,
          isManualOverride: seat.manualOverride ?? false,
          usn: seat.usn ?? null,
          studentName: seat.studentName ?? null,
          branchCode: seat.branchCode ?? null,
        })
      }
    })
    return map
  }, [assignedSeats, removedAssignments])

  const pendingByPosition = useMemo(() => {
    const map = new Map<string, SeatDto>()
    pendingAssignments.forEach((seat) => {
      const key = `${seat.benchRow}:${seat.benchCol}:${seat.benchSeatIndex}`
      map.set(key, seat)
    })
    return map
  }, [pendingAssignments])

  const gridRows = hallGrid.grid ?? []
  const benches = useMemo(() => {
    return gridRows
      .flatMap((row) => row)
      .map((cell) => normalizeBench(cell, hallGrid.hallId, assignedByPosition, pendingByPosition))
  }, [gridRows, hallGrid.hallId, assignedByPosition, pendingByPosition])

  const stats = useMemo(() => {
    const totalCapacity = benches.reduce((sum, bench) => sum + bench.seats.length, 0)
    const occupiedCount = benches.reduce((sum, bench) => sum + bench.occupancyCount, 0)
    return {
      totalCapacity,
      occupiedCount,
      utilizationPercent:
        totalCapacity > 0 ? Math.round((occupiedCount / totalCapacity) * 100) : 0,
    }
  }, [benches])

  const warnings = useMemo(
    () => gridRows.flatMap((row) => row.flatMap((cell) => cell.warnings ?? [])).slice(0, 3),
    [gridRows]
  )

  return (
    <div className="flex flex-col gap-4 border-2 border-gray-300 rounded-lg p-4 bg-gray-50">
      {/* Hall Header */}
      <div className="flex justify-between items-start">
        <div>
          <h3 className="text-lg font-bold text-gray-800">
            {hallGrid.roomDisplayName || hallGrid.roomName || 'Unknown room'}
          </h3>
          <div className="text-sm text-gray-600">
            {hallGrid.benchRows ?? 0} rows · {hallGrid.benchCols ?? 0} columns
          </div>
        </div>

        {/* Stats */}
        <div className="text-right">
          <div className="text-2xl font-bold text-green-600">
            {stats.occupiedCount}/{stats.totalCapacity}
          </div>
          <div className="text-sm text-gray-600">
            {stats.utilizationPercent}% full
          </div>
        </div>
      </div>

      {/* Warnings */}
      {warnings.length > 0 && (
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-3">
          <div className="text-sm font-semibold text-yellow-800 mb-1">Warnings</div>
          <ul className="text-xs text-yellow-700 space-y-1">
            {warnings.map((warning, idx) => (
              <li key={idx}>
                • {warning.type}: {warning.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* 2D Grid */}
      <div
        className="grid gap-3 p-4 bg-white rounded border-2 border-gray-200 overflow-auto max-h-96"
        style={{
          gridTemplateColumns: `repeat(${hallGrid.benchCols ?? 1}, minmax(120px, 1fr))`,
          gridAutoRows: 'minmax(120px, auto)',
        }}
        onMouseLeave={() => setHoveredSeatId(null)}
      >
        {benches.map((bench) => (
          <BenchCell
            key={`${bench.row}-${bench.col}-${bench.benchNumber}`}
            bench={bench}
            selectedStudent={selectedStudent}
            onSeatClick={onSeatClick}
            pendingAssignments={pendingAssignments}
            style={{
              gridColumnStart: bench.col,
              gridRowStart: bench.row,
            }}
          />
        ))}
      </div>

      {/* Legend */}
      <div className="flex gap-4 text-xs text-gray-600 flex-wrap">
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-blue-100 border border-blue-300 rounded" />
          <span>CSE</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-green-100 border border-green-300 rounded" />
          <span>ISE</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-yellow-100 border border-yellow-300 rounded" />
          <span>ECE</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-red-100 border border-red-300 rounded" />
          <span>MECH</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-gray-100 border border-gray-300 rounded" />
          <span>Empty</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-4 h-4 bg-gray-300 border border-gray-500 rounded" />
          <span>Excluded</span>
        </div>
      </div>
    </div>
  )
}
