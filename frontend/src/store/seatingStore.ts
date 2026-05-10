/**
 * Zustand store for seating dashboard state management
 * Handles UI interactions: student selection, seat focus, undo/redo
 */

import { create } from 'zustand'
import type { SeatDto, UnassignedStudentDto } from '@/types/exam'

interface DashboardStore {
  // Selection state
  selectedStudent: UnassignedStudentDto | null
  setSelectedStudent: (student: UnassignedStudentDto | null) => void

  // Hover state for UI feedback
  hoveredSeatId: string | null
  setHoveredSeatId: (seatId: string | null) => void

  // Current hall being edited
  activeHallId: string | null
  setActiveHallId: (hallId: string | null) => void

  // Pending seat assignments (not yet saved to backend)
  pendingAssignments: Map<string, SeatDto>
  addPendingAssignment: (seat: SeatDto) => void
  addPendingAssignments: (seats: SeatDto[]) => void
  removePendingAssignment: (seatId: string) => void
  clearPendingAssignments: () => void
  getPendingAssignment: (seatId: string) => SeatDto | undefined

  // Pending removals of existing seat placements
  removedAssignments: Set<string>
  addRemovedAssignment: (positionKey: string) => void
  removeRemovedAssignment: (positionKey: string) => void
  clearRemovedAssignments: () => void

  // Auto-save debounce flag
  isDirty: boolean
  setIsDirty: (dirty: boolean) => void

  // Warnings display
  showWarnings: boolean
  setShowWarnings: (show: boolean) => void
}

export const useDashboardStore = create<DashboardStore>((set, get) => ({
  selectedStudent: null,
  setSelectedStudent: (student) => set({ selectedStudent: student }),

  hoveredSeatId: null,
  setHoveredSeatId: (seatId) => set({ hoveredSeatId: seatId }),

  activeHallId: null,
  setActiveHallId: (hallId) => set({ activeHallId: hallId }),

  pendingAssignments: new Map(),
  addPendingAssignment: (seat) => {
    const current = get().pendingAssignments
    const updated = new Map(current)
    updated.set(seat.seatId, seat)
    set({ pendingAssignments: updated, isDirty: true })
  },
  addPendingAssignments: (seats) => {
    const current = get().pendingAssignments
    const updated = new Map(current)
    seats.forEach(seat => updated.set(seat.seatId, seat))
    set({ pendingAssignments: updated, isDirty: true })
  },
  removePendingAssignment: (seatId) => {
    const current = get().pendingAssignments
    const updated = new Map(current)
    updated.delete(seatId)
    set({ pendingAssignments: updated, isDirty: true })
  },
  clearPendingAssignments: () => set({ pendingAssignments: new Map(), isDirty: false }),
  getPendingAssignment: (seatId) => get().pendingAssignments.get(seatId),

  removedAssignments: new Set(),
  addRemovedAssignment: (positionKey) => {
    const current = get().removedAssignments
    const updated = new Set(current)
    updated.add(positionKey)
    set({ removedAssignments: updated, isDirty: true })
  },
  removeRemovedAssignment: (positionKey) => {
    const current = get().removedAssignments
    const updated = new Set(current)
    updated.delete(positionKey)
    set({ removedAssignments: updated, isDirty: true })
  },
  clearRemovedAssignments: () => set({ removedAssignments: new Set(), isDirty: false }),

  isDirty: false,
  setIsDirty: (dirty) => set({ isDirty: dirty }),

  showWarnings: true,
  setShowWarnings: (show) => set({ showWarnings: show }),
}))
