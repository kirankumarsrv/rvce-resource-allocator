/**
 * Seating Allocation Rule Engine
 * Handles rule-based student seating assignments
 */

import type { SeatDto, StudentGroupDto, ExamHallDto, HallGridDto, ExamSeatDto } from '@/types/exam'
import type { AllocationRuleId } from '@/components/AllocationRuleCard'

export const getStrategyIndices = (ruleId: AllocationRuleId): number[] => {
  const strategies: Record<AllocationRuleId, number[]> = {
    'two-seater-left': [0],
    'two-seater-right': [1],
    'three-seater-middle': [1],
    'three-seater-left-corner': [0],
    'three-seater-right-corner': [2],
    'three-seater-corners': [0, 2],
  }
  return strategies[ruleId] ?? []
}

interface AllocationContext {
  assignedSeats: (SeatDto | ExamSeatDto)[]
  pendingAssignments: Map<string, SeatDto>
  removedAssignments: Set<string>
  positionKey: (seat: { benchRow: number; benchCol: number; benchSeatIndex: number }) => string
}

/**
 * Apply allocation rule to assign students to available seats
 * Ensures type-specific matching: 2-seater rules only match 2-seater benches, etc.
 */
export const applyAllocationRule = (
  hall: ExamHallDto,
  hallGrid: HallGridDto,
  studentGroup: StudentGroupDto,
  ruleId: AllocationRuleId,
  context: AllocationContext
): SeatDto[] => {
  const ruleIndices = getStrategyIndices(ruleId)
  if (!ruleIndices) return []

  const validStudents = studentGroup.students.filter((s) => s.studentId)
  if (validStudents.length === 0) return []

  // Build occupied positions map
  const occupiedByPosition = new Map<string, SeatDto>()
  context.assignedSeats.forEach((seat) => {
    const key = context.positionKey(seat)
    if (!context.removedAssignments.has(key)) {
      occupiedByPosition.set(key, {
        ...seat,
        usn: seat.usn ?? null,
        studentName: seat.studentName ?? null,
        branchCode: seat.branchCode ?? null,
        isManualOverride: ('isManualOverride' in seat) ? seat.isManualOverride : seat.manualOverride ?? false,
      })
    }
  })
  context.pendingAssignments.forEach((seat) => occupiedByPosition.set(context.positionKey(seat), seat))

  // Collect all available seats that match the rule pattern
  const availableSeats: Array<SeatDto & { seatCapacity: number }> = []
  hallGrid.grid.forEach((row) => {
    row.forEach((cell) => {
      if (cell.excluded) return

      const isThreeSeaterRule = ruleId.startsWith('three-seater')
      const isTwoSeaterRule = ruleId.startsWith('two-seater')

      // Type-specific matching: only match benches of the correct size
      if (isThreeSeaterRule && cell.seatCapacity !== 3) return
      if (isTwoSeaterRule && cell.seatCapacity !== 2) return

      for (let index = 0; index < cell.seatCapacity; index += 1) {
        // Check if this seat index matches the rule
        if (!ruleIndices.includes(index)) continue

        const key = `${cell.row}:${cell.col}:${index}`
        if (occupiedByPosition.has(key)) continue

        availableSeats.push({
          seatId: `bulk-${hall.hallId}-${cell.row}-${cell.col}-${index}`,
          examId: hall.examId,
          studentId: null,
          hallId: hall.hallId,
          benchRow: cell.row,
          benchCol: cell.col,
          benchSeatIndex: index,
          benchNumber: cell.label,
          usn: null,
          studentName: null,
          branchCode: null,
          isManualOverride: true,
          seatCapacity: cell.seatCapacity,
        })
      }
    })
  })

  // Sort available seats by row, column, then seat index for predictable physical placement
  availableSeats.sort(
    (a, b) =>
      a.benchRow - b.benchRow || a.benchCol - b.benchCol || a.benchSeatIndex - b.benchSeatIndex
  )

  // Assign students to available seats
  const assignments: SeatDto[] = []
  const studentsToAssign = [...validStudents]

  for (const seat of availableSeats) {
    if (studentsToAssign.length === 0) break

    const student = studentsToAssign.shift()!
    const assignment: SeatDto = {
      ...seat,
      studentId: student.studentId,
      usn: student.usn,
      studentName: student.studentName,
      branchCode: student.branchCode,
      isManualOverride: true,
    }
    assignments.push(assignment)
  }

  return assignments
}
