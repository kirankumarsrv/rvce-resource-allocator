/**
 * API Service for T-105: Timetable Queries and Analytics
 */

import {
  TeacherScheduleDto,
  RoomScheduleDto,
  TimetableAnalyticsDto,
  TeacherWeeklySchedule,
  RoomWeeklySchedule,
  RoomAvailabilityDto,
  RoomAvailabilityQuery,
  UploadResultDto,
  SimpleDto,
  SubstituteRequest,
  SubstitutionResultDto,
  OverrideDto,
  OverrideRequest,
} from '../types/timetable'
import { authenticatedFetch } from '@/services/authService'

const API_BASE = '/api/timetable'

/**
 * Get teacher's schedule for a specific day of week
 */
export const getTeacherSchedule = async (
  teacherId: string,
  dayOfWeek: number
): Promise<TeacherScheduleDto[]> => {
  const response = await authenticatedFetch(
    `${API_BASE}/teacher/${teacherId}/schedule?dayOfWeek=${dayOfWeek}`
  )
  if (!response.ok) throw new Error('Failed to fetch teacher schedule')
  return response.json()
};

/**
 * Get teacher's full weekly schedule (all 7 days)
 */
export const getTeacherWeeklySchedule = async (
  teacherId: string
): Promise<TeacherWeeklySchedule> => {
  const response = await authenticatedFetch(
    `${API_BASE}/teacher/${teacherId}/schedule/weekly`
  )
  if (!response.ok) throw new Error('Failed to fetch teacher weekly schedule')
  return response.json()
};

/**
 * Get room's schedule for a specific day of week
 */
export const getRoomSchedule = async (
  roomId: number,
  dayOfWeek: number
): Promise<RoomScheduleDto[]> => {
  const response = await authenticatedFetch(
    `${API_BASE}/room/${roomId}/schedule?dayOfWeek=${dayOfWeek}`
  )
  if (!response.ok) throw new Error('Failed to fetch room schedule')
  return response.json()
};

/**
 * Get room's full weekly schedule (all 7 days)
 */
export const getRoomWeeklySchedule = async (
  roomId: number
): Promise<RoomWeeklySchedule> => {
  const response = await authenticatedFetch(
    `${API_BASE}/room/${roomId}/schedule/weekly`
  )
  if (!response.ok) throw new Error('Failed to fetch room weekly schedule')
  return response.json()
};

/**
 * Get timetable analytics and utilization metrics
 */
export const getTimetableAnalytics = async (): Promise<TimetableAnalyticsDto> => {
  const response = await authenticatedFetch(`${API_BASE}/analytics`)
  if (!response.ok) throw new Error('Failed to fetch analytics')
  return response.json()
};

/**
 * Get available rooms for the selected time slot (T-102)
 * Queries the availability query engine with optional filters
 */
export const getAvailableRooms = async (
  query: RoomAvailabilityQuery
): Promise<RoomAvailabilityDto[]> => {
  const params = new URLSearchParams();

  if (query.date) params.append('date', query.date);
  params.append('startTime', query.startTime);
  params.append('endTime', query.endTime);
  if (query.minCapacity) params.append('minCapacity', query.minCapacity.toString());
  if (query.building) params.append('building', query.building);

  const response = await authenticatedFetch(`${API_BASE}/available?${params.toString()}`)

  if (!response.ok) throw new Error('Failed to fetch available rooms')
  return response.json()
};

export const uploadTimetable = async (file: File): Promise<UploadResultDto> => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await authenticatedFetch(`${API_BASE}/upload`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    let message = 'Failed to upload timetable'
    try {
      const payload = await response.json()
      if (Array.isArray(payload?.errors) && payload.errors.length > 0) {
        message = payload.errors.join('; ')
      } else if (typeof payload?.message === 'string' && payload.message.trim()) {
        message = payload.message.trim()
      } else if (typeof payload?.error === 'string' && payload.error.trim()) {
        message = payload.error.trim()
      }
    } catch {
      const errorText = await response.text()
      if (errorText) {
        message = errorText
      }
    }
    throw new Error(message)
  }

  return response.json()
};

export const getTeachers = async (): Promise<SimpleDto[]> => {
  const response = await authenticatedFetch(`${API_BASE}/teachers`)

  if (!response.ok) {
    let message = 'Failed to fetch teachers'
    try {
      const payload = await response.json()
      if (typeof payload?.message === 'string' && payload.message.trim()) {
        message = payload.message.trim()
      } else if (typeof payload?.error === 'string' && payload.error.trim()) {
        message = payload.error.trim()
      }
    } catch {
      const errorText = await response.text()
      if (errorText) {
        message = errorText
      }
    }
    throw new Error(message)
  }
  return response.json()
};

export const substituteTeacher = async (
  request: SubstituteRequest
): Promise<SubstitutionResultDto> => {
  const response = await authenticatedFetch(`${API_BASE}/substitute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Failed to submit substitution')
  }

  return response.json()
};

export const getOverrides = async (date: string, roomId?: string): Promise<OverrideDto[]> => {
  const params = new URLSearchParams({ date });
  if (roomId) params.append('roomId', roomId);
  const response = await authenticatedFetch(`${API_BASE}/overrides?${params.toString()}`);
  if (!response.ok) throw new Error('Failed to fetch overrides');
  return response.json();
};

export const createOverride = async (request: OverrideRequest): Promise<OverrideDto> => {
  const response = await authenticatedFetch(`${API_BASE}/overrides`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) throw new Error('Failed to create override');
  return response.json();
};

export const deleteOverride = async (id: string): Promise<void> => {
  const response = await authenticatedFetch(`${API_BASE}/overrides/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to delete override');
};
