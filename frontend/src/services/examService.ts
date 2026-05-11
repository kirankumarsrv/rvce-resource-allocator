/**
 * API Service for exam seating (T-401 through T-405)
 */

import { authenticatedFetch } from '@/services/authService'
import type {
  CreateExamSessionRequest,
  ExamSessionDto,
  ExamHallDto,
  SeatingDashboardStateDto,
  SeatingSessionDto,
  BulkSeatSaveRequest,
  HallGridDto,
  ExamHallConfigRequest,
} from '@/types/exam'
import type { RoomAvailabilityDto } from '@/types/timetable'

const API_BASE = 'http://localhost:8080/api/exam'

/**
 * Create a new exam session
 */
export const createExamSession = async (
  examData: CreateExamSessionRequest
): Promise<ExamSessionDto> => {
  const response = await authenticatedFetch(`${API_BASE}/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(examData),
  })
  if (!response.ok) {
    let details = ''
    try {
      details = (await response.text()).trim()
    } catch {
      details = ''
    }

    const fallbackByStatus: Record<number, string> = {
      400: 'Bad Request',
      401: 'Unauthorized',
      403: 'Forbidden',
      404: 'Not Found',
      409: 'Conflict',
      500: 'Internal Server Error',
    }
    const statusReason = fallbackByStatus[response.status] ?? 'Request failed'
    const statusLine = `HTTP ${response.status} ${statusReason}`
    throw new Error(details ? `${statusLine}: ${details}` : statusLine)
  }
  return response.json()
}

/**
 * Search departments for autocomplete
 */
export const searchDepartments = async (q = ''): Promise<{ id: string; text: string }[]> => {
  const url = `http://localhost:8080/api/departments?q=${encodeURIComponent(q)}`
  const response = await authenticatedFetch(url)
  if (!response.ok) return []
  return response.json()
}

/**
 * List available rooms for exam hall assignment
 */
export const listAvailableRooms = async (examId: string): Promise<RoomAvailabilityDto[]> => {
  const url = `http://localhost:8080/api/exam/${examId}/rooms/available`
  const response = await authenticatedFetch(url)
  if (!response.ok) return []
  return response.json()
}

/**
 * Get all exam sessions (paginated)
 */
export const listExamSessions = async (page = 0, size = 10): Promise<{
  content: ExamSessionDto[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}> => {
  const response = await authenticatedFetch(`${API_BASE}/sessions?page=${page}&size=${size}`)
  if (!response.ok) {
    let details = ''
    try {
      details = (await response.text()).trim()
    } catch {
      details = ''
    }

    const fallbackByStatus: Record<number, string> = {
      401: 'Unauthorized',
      403: 'Forbidden',
      404: 'Not Found',
      500: 'Internal Server Error',
    }
    const statusReason = fallbackByStatus[response.status] ?? 'Request failed'
    const statusLine = `HTTP ${response.status} ${statusReason}`
    throw new Error(details ? `${statusLine}: ${details}` : statusLine)
  }
  return response.json()
}

/**
 * Get a specific exam session
 */
export const getExamSession = async (examId: string): Promise<ExamSessionDto> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}`)
  if (!response.ok) throw new Error('Failed to fetch exam session')
  return response.json()
}

/**
 * Upload students CSV for an exam
 */
export const uploadStudentsCSV = async (examId: string, file: File): Promise<{
  totalRows: number
  inserted: number
  skipped: number
  errors: Array<{ row: number; usn: string; error: string }>
}> => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await authenticatedFetch(
    `${API_BASE}/students/upload?examId=${examId}`,
    {
      method: 'POST',
      body: formData,
    }
  )
  if (!response.ok) {
    let errorMessage = 'Failed to upload students'
    try {
      const errorData = await response.json()
      if (Array.isArray(errorData.errors) && errorData.errors.length > 0) {
        const firstErrors = errorData.errors.slice(0, 5).map((item: { row?: number; usn?: string; error?: string }) => {
          const rowLabel = item.row ? `row ${item.row}` : 'row ?'
          const usnLabel = item.usn ? ` [${item.usn}]` : ''
          return `${rowLabel}${usnLabel}: ${item.error ?? 'Unknown error'}`
        })
        errorMessage = `CSV validation failed (${errorData.inserted ?? 0} inserted, ${errorData.skipped ?? 0} skipped): ${firstErrors.join('; ')}`
      } else if (typeof errorData.message === 'string' && errorData.message.trim()) {
        errorMessage = errorData.message.trim()
      } else if (typeof errorData.error === 'string' && errorData.error.trim()) {
        errorMessage = errorData.error.trim()
      }
    } catch {
      try {
        const errorText = await response.text()
        if (errorText.trim()) {
          errorMessage = errorText.trim()
        }
      } catch {
        // Keep default message.
      }
    }
    throw new Error(errorMessage)
  }
  return response.json()
}

/**
 * Add a hall to an exam
 */
export const addExamHall = async (
  examId: string,
  hallData: ExamHallConfigRequest
): Promise<ExamHallDto> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/halls`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(hallData),
  })
  if (!response.ok) {
    let errorMessage = 'Failed to add exam hall'
    try {
      const errorData = await response.json()
      if (errorData.message) {
        errorMessage = errorData.message
      } else if (errorData.error) {
        errorMessage = errorData.error
      }
    } catch {
      // If parsing fails, try text
      try {
        const errorText = await response.text()
        if (errorText.trim()) {
          errorMessage = errorText.trim()
        }
      } catch {
        // Keep default message
      }
    }
    throw new Error(errorMessage)
  }
  return response.json()
}

/**
 * Get all halls for an exam
 */
export const listExamHalls = async (examId: string): Promise<ExamHallDto[]> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/halls`)
  if (!response.ok) throw new Error('Failed to fetch exam halls')
  return response.json()
}

/**
 * Get the 2D grid for a hall
 */
export const getHallGrid = async (examId: string, hallId: string): Promise<HallGridDto> => {
  const response = await authenticatedFetch(
    `${API_BASE}/${examId}/halls/${hallId}/grid`
  )
  if (!response.ok) throw new Error('Failed to fetch hall grid')
  return response.json()
}

/**
 * Open a seating session
 */
export const openSeatingSession = async (examId: string): Promise<SeatingSessionDto> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/seating/session`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!response.ok) throw new Error('Failed to open seating session')
  return response.json()
}

/**
 * Load dashboard state (unassigned students + all halls + grid layouts)
 */
export const loadSeatingDashboardState = async (
  examId: string
): Promise<SeatingDashboardStateDto> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/seating/state`)
  if (!response.ok) throw new Error('Failed to load seating dashboard state')
  return response.json()
}

/**
 * Bulk save seat assignments
 */
export const bulkSaveSeats = async (
  examId: string,
  saveRequest: BulkSeatSaveRequest
): Promise<SeatingDashboardStateDto> => {
  const response = await authenticatedFetch(
    `${API_BASE}/${examId}/seats/bulk-save`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(saveRequest),
    }
  )
  if (!response.ok) {
    let message = 'Failed to save seats'
    try {
      const payload = await response.json()
      if (payload?.message) {
        message = payload.message
      } else if (payload?.error) {
        message = payload.error
      }
    } catch {
      try {
        const text = await response.text()
        if (text.trim()) {
          message = text.trim()
        }
      } catch {
        // keep default message
      }
    }
    throw new Error(message)
  }
  return response.json()
}

/**
 * Clear all assignments from a hall
 */
export const clearHall = async (examId: string, hallId: string): Promise<void> => {
  const response = await authenticatedFetch(
    `${API_BASE}/${examId}/seats/clear-hall/${hallId}`,
    { method: 'DELETE' }
  )
  if (!response.ok) throw new Error('Failed to clear hall')
}

/**
 * Delete an exam hall completely
 */
export const deleteExamHall = async (examId: string, hallId: string): Promise<void> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/halls/${hallId}`, {
    method: 'DELETE',
  })
  if (!response.ok) throw new Error('Failed to delete hall')
}

/**
 * Publish exam seating
 */
export const publishExam = async (examId: string): Promise<ExamSessionDto> => {
  const response = await authenticatedFetch(`${API_BASE}/${examId}/publish`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!response.ok) throw new Error('Failed to publish exam')
  return response.json()
}
