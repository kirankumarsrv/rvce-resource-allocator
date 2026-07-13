import { authenticatedFetch } from '@/services/authService'
import type { DepartmentInput, SchedulerResult, ScheduledSlot } from '@/types/scheduler'

const API_BASE = '/api/scheduler'

/**
 * POST /api/scheduler/generate
 * Generates a PREVIEW timetable for the given department input. This does
 * NOT persist anything to the database — call confirmTimetable() once the
 * TTO has reviewed the result and wants to save it.
 */
export const generateTimetable = async (
  input: DepartmentInput
): Promise<SchedulerResult> => {
  const response = await authenticatedFetch(`${API_BASE}/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    let message = 'Failed to generate timetable'
    try {
      const payload = await response.json()
      if (typeof payload?.message === 'string' && payload.message.trim()) {
        message = payload.message.trim()
      } else if (typeof payload?.error === 'string' && payload.error.trim()) {
        message = payload.error.trim()
      }
    } catch {
      const text = await response.text()
      if (text) message = text
    }
    throw new Error(message)
  }

  return response.json()
}

export interface ConfirmResponse {
  versionId: string
  savedSlots: number
}

/**
 * POST /api/scheduler/confirm
 * Persists a previously generated result to the database. This is the only
 * scheduler call that actually writes to timetable_slots.
 */
export const confirmTimetable = async (
  department: string,
  scheduledSlots: ScheduledSlot[]
): Promise<ConfirmResponse> => {
  const response = await authenticatedFetch(`${API_BASE}/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ department, scheduledSlots }),
  })

  if (!response.ok) {
    let message = 'Failed to confirm timetable'
    try {
      const payload = await response.json()
      if (typeof payload?.message === 'string' && payload.message.trim()) {
        message = payload.message.trim()
      } else if (typeof payload?.error === 'string' && payload.error.trim()) {
        message = payload.error.trim()
      }
    } catch {
      const text = await response.text()
      if (text) message = text
    }
    throw new Error(message)
  }

  return response.json()
}

/*
 * NOTE — Missing read endpoint:
 * The backend still only exposes POST /generate (preview) and POST /confirm (save).
 * There is no GET endpoint to retrieve a previously confirmed timetable by department
 * without re-generating. Recommended next addition:
 *   GET /api/scheduler/results?department={dept}
 * or reuse existing timetable query endpoints filtered by department.
 */