import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BenchCell } from './BenchCell'
import { ClassroomGrid } from './ClassroomGrid'
import type { BenchCellDto, ExamSeatDto, HallGridDto } from '@/types/exam'

const sampleBench: BenchCellDto = {
  row: 1,
  col: 1,
  benchNumber: 'A-1',
  benchType: 2,
  seats: [
    {
      seatId: 'seat-1',
      examId: 'exam-1',
      studentId: null,
      hallId: 'hall-1',
      benchRow: 1,
      benchCol: 1,
      benchSeatIndex: 0,
      benchNumber: 'A-1',
      usn: null,
      studentName: null,
      branchCode: null,
      
      isManualOverride: false,
    },
    {
      seatId: 'seat-2',
      examId: 'exam-1',
      studentId: null,
      hallId: 'hall-1',
      benchRow: 1,
      benchCol: 1,
      benchSeatIndex: 1,
      benchNumber: 'A-1',
      usn: null,
      studentName: null,
      branchCode: null,
      
      isManualOverride: false,
    },
  ],
  occupancyCount: 0,
  excluded: false,
}

const excludedBench: BenchCellDto = {
  row: 1,
  col: 1,
  benchNumber: 'A-2',
  benchType: 2,
  seats: [],
  occupancyCount: 0,
  excluded: true,
}

describe('BenchCell', () => {
  it('renders an excluded bench with its label', () => {
    render(
      <BenchCell
        bench={excludedBench}
        selectedStudent={null}
        onSeatClick={vi.fn()}
        pendingAssignments={new Map()}
      />
    )

    expect(screen.getByText('EXCLUDED')).toBeInTheDocument()
    expect(screen.getByText('A-2')).toBeInTheDocument()
  })

  it('calls onSeatClick when an empty seat is clicked', () => {
    const onSeatClick = vi.fn()

    render(
      <BenchCell
        bench={sampleBench}
        selectedStudent={null}
        onSeatClick={onSeatClick}
        pendingAssignments={new Map()}
      />
    )

    const seatButtons = screen.getAllByRole('button', { name: /empty/i })
    fireEvent.click(seatButtons[0])

    expect(onSeatClick).toHaveBeenCalledTimes(1)
    expect(onSeatClick).toHaveBeenCalledWith(sampleBench.seats[0], null)
  })
})

describe('ClassroomGrid', () => {
  const assignedSeat: ExamSeatDto = {
    seatId: 'seat-3',
    examId: 'exam-1',
    hallId: 'hall-1',
    studentId: 'student-1',
    benchRow: 1,
    benchCol: 1,
    benchSeatIndex: 0,
    benchNumber: 'A-1',
    manualOverride: false,
    usn: 'USN001',
    studentName: 'Alice Example',
    branchCode: 'CSE',
    
  }

  const hallGrid: HallGridDto = {
    hallId: 'hall-1',
    roomName: 'Room 101',
    roomDisplayName: 'Main Hall',
    benchRows: 1,
    benchCols: 1,
    grid: [
      [
        {
          row: 1,
          col: 1,
          label: 'A-1',
          seatCapacity: 2,
          occupiedCount: 1,
          active: true,
          excluded: false,
          seats: [],
          warnings: [],
        },
      ],
    ],
  }

  it('renders hall header, occupancy stats, and assigned student seat', () => {
    render(
      <ClassroomGrid
        hallGrid={hallGrid}
        assignedSeats={[assignedSeat]}
        selectedStudent={null}
        pendingAssignments={new Map()}
        removedAssignments={new Set()}
        onSeatClick={vi.fn()}
      />
    )

    expect(screen.getByText('Main Hall')).toBeInTheDocument()
    expect(screen.getAllByText('1/2')[0]).toBeInTheDocument()
    expect(screen.getByText('50% full')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(screen.getByText('USN001')).toBeInTheDocument()
  })

  it('forwards clicks on generated seats to the onSeatClick handler', () => {
    const onSeatClick = vi.fn()

    render(
      <ClassroomGrid
        hallGrid={hallGrid}
        assignedSeats={[assignedSeat]}
        selectedStudent={null}
        pendingAssignments={new Map()}
        removedAssignments={new Set()}
        onSeatClick={onSeatClick}
      />
    )

    const seatButton = screen.getAllByRole('button').find((button) =>
      button.textContent?.includes('Alice')
    )

    expect(seatButton).toBeDefined()
    if (seatButton) {
      fireEvent.click(seatButton)
      expect(onSeatClick).toHaveBeenCalledTimes(1)
    }
  })
})

