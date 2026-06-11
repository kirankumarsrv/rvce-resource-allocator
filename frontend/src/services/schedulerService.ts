import { authenticatedFetch } from '@/services/authService'
import type { DepartmentInput, SchedulerResult } from '@/types/scheduler'

const API_BASE = '/api/scheduler'

/**
 * POST /api/scheduler/generate
 * Generates a timetable for the given department input and persists it to the DB.
 * Returns the full SchedulerResult including scheduled slots, teacher load, and
 * any unscheduled hours.
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

/*
 * NOTE — Missing read endpoint:
 * The backend SchedulerController currently only exposes POST /api/scheduler/generate.
 * There is no GET endpoint to retrieve previously generated timetables by department.
 * The generated result is persisted to the timetable_slots table, so the data exists,
 * but there is no scheduler-specific read API for the frontend to query.
 *
 * Recommended next addition:
 *   GET /api/scheduler/results?department={dept}
 * or reuse the existing timetable query endpoints
 *   GET /api/timetable/... filtered by department
 * to surface persisted scheduler output without re-generating.
 *
 * Until that endpoint exists this service only calls /generate and the frontend
 * keeps the result in local state for the current session.
 */
