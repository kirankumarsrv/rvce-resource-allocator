import { describe, expect, it } from 'vitest'
import { applyAllocationRule, getStrategyIndices } from './allocationRules'
import type { ExamHallDto, HallGridDto } from '@/types/exam'

describe('Allocation Rules Engine', () => {
  describe('getStrategyIndices', () => {
    it('returns correct seat indices for each rule', () => {
      expect(getStrategyIndices('two-seater-left')).toEqual([0])
      expect(getStrategyIndices('two-seater-right')).toEqual([1])
      expect(getStrategyIndices('three-seater-middle')).toEqual([1])
      expect(getStrategyIndices('three-seater-left-corner')).toEqual([0])
      expect(getStrategyIndices('three-seater-right-corner')).toEqual([2])
      expect(getStrategyIndices('three-seater-corners')).toEqual([0, 2])
    })
  })

  describe('applyAllocationRule - Bench Type Isolation (Regression)', () => {
    const createHall = (hallId: string = 'hall-1'): ExamHallDto => ({
      hallId,
      examId: 'exam-1',
      roomId: 'room-1',
      roomName: 'Room A',
      roomDisplayName: 'Main Hall A',
      twoSeaterCount: 2,
      threeSeaterCount: 2,
      totalCapacity: 10,
      benchRows: 2,
      benchCols: 2,
      invigilatorId: null,
      invigilatorName: null,
    })

    const createHallGrid = (): HallGridDto => ({
      hallId: 'hall-1',
      roomName: 'Room A',
      roomDisplayName: 'Main Hall A',
      benchRows: 2,
      benchCols: 2,
      grid: [
        [
          {
            row: 1,
            col: 1,
            label: 'A1',
            seatCapacity: 2,
            occupiedCount: 0,
            active: true,
            excluded: false,
            seats: [],
            warnings: [],
          },
          {
            row: 1,
            col: 2,
            label: 'A2',
            seatCapacity: 3,
            occupiedCount: 0,
            active: true,
            excluded: false,
            seats: [],
            warnings: [],
          },
        ],
        [
          {
            row: 2,
            col: 1,
            label: 'B1',
            seatCapacity: 3,
            occupiedCount: 0,
            active: true,
            excluded: false,
            seats: [],
            warnings: [],
          },
          {
            row: 2,
            col: 2,
            label: 'B2',
            seatCapacity: 2,
            occupiedCount: 0,
            active: true,
            excluded: false,
            seats: [],
            warnings: [],
          },
        ],
      ],
    })

    const positionKey = (seat: { benchRow: number; benchCol: number; benchSeatIndex: number }) =>
      `${seat.benchRow}:${seat.benchCol}:${seat.benchSeatIndex}`

    it('3-seater-left-corner rule should only match 3-seater benches, not 2-seater', () => {
      const hall = createHall()
      const hallGrid = createHallGrid()

      const assignments = applyAllocationRule(
        hall,
        hallGrid,
        {
          groupId: 'group-1',
          label: 'Test Group',
          students: [
            {
              entryId: 'entry-1',
              usn: 'USN001',
              studentName: 'Student 1',
              studentId: 'student-1',
              branchCode: 'CSE',
            },
          ],
        },
        'three-seater-left-corner',
        {
          assignedSeats: [],
          pendingAssignments: new Map(),
          removedAssignments: new Set(),
          positionKey,
        }
      )

      expect(assignments).toHaveLength(1)

      // Check it matched a 3-seater bench, not a 2-seater
      const assignment = assignments[0]
      const [benchRow, benchCol] = [assignment.benchRow, assignment.benchCol]
      const isBench3Seater =
        (benchRow === 1 && benchCol === 2) || (benchRow === 2 && benchCol === 1)

      expect(isBench3Seater).toBe(true)
      expect(assignment.benchSeatIndex).toBe(0) // Left corner is index 0
    })

    it('3-seater-middle rule should only match 3-seater benches, not 2-seater', () => {
      const hall = createHall()
      const hallGrid = createHallGrid()

      const assignments = applyAllocationRule(
        hall,
        hallGrid,
        {
          groupId: 'group-1',
          label: 'Test Group',
          students: [
            {
              entryId: 'entry-1',
              usn: 'USN001',
              studentName: 'Student 1',
              studentId: 'student-1',
              branchCode: 'CSE',
            },
          ],
        },
        'three-seater-middle',
        {
          assignedSeats: [],
          pendingAssignments: new Map(),
          removedAssignments: new Set(),
          positionKey,
        }
      )

      expect(assignments).toHaveLength(1)

      // Check it matched a 3-seater bench
      const assignment = assignments[0]
      const [benchRow, benchCol] = [assignment.benchRow, assignment.benchCol]
      const isBench3Seater =
        (benchRow === 1 && benchCol === 2) || (benchRow === 2 && benchCol === 1)

      expect(isBench3Seater).toBe(true)
      expect(assignment.benchSeatIndex).toBe(1) // Middle is index 1
    })

    it('2-seater-left rule should only match 2-seater benches, not 3-seater', () => {
      const hall = createHall()
      const hallGrid = createHallGrid()

      const assignments = applyAllocationRule(
        hall,
        hallGrid,
        {
          groupId: 'group-1',
          label: 'Test Group',
          students: [
            {
              entryId: 'entry-1',
              usn: 'USN001',
              studentName: 'Student 1',
              studentId: 'student-1',
              branchCode: 'CSE',
            },
          ],
        },
        'two-seater-left',
        {
          assignedSeats: [],
          pendingAssignments: new Map(),
          removedAssignments: new Set(),
          positionKey,
        }
      )

      expect(assignments).toHaveLength(1)

      // Check it matched a 2-seater bench
      const assignment = assignments[0]
      const [benchRow, benchCol] = [assignment.benchRow, assignment.benchCol]
      const isBench2Seater = (benchRow === 1 && benchCol === 1) || (benchRow === 2 && benchCol === 2)

      expect(isBench2Seater).toBe(true)
      expect(assignment.benchSeatIndex).toBe(0) // Left is index 0
    })

    it('applies seats in correct physical order (row → col → index)', () => {
      const hall = createHall()
      const hallGrid = createHallGrid()

      const assignments = applyAllocationRule(
        hall,
        hallGrid,
        {
          groupId: 'group-1',
          label: 'Test Group',
          students: [
            { entryId: 'e1', usn: 'S1', studentName: 'Student 1', studentId: 'sid1', branchCode: 'CSE' },
            { entryId: 'e2', usn: 'S2', studentName: 'Student 2', studentId: 'sid2', branchCode: 'ISE' },
          ],
        },
        'three-seater-left-corner',
        {
          assignedSeats: [],
          pendingAssignments: new Map(),
          removedAssignments: new Set(),
          positionKey,
        }
      )

      // Should fill A2 first (row 1, col 2), then B1 (row 2, col 1)
      expect(assignments).toHaveLength(2)
      expect(assignments[0].benchRow).toBe(1)
      expect(assignments[0].benchCol).toBe(2)
      expect(assignments[1].benchRow).toBe(2)
      expect(assignments[1].benchCol).toBe(1)
    })
  })
})
