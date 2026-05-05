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
  OverrideRequest,
  OverrideDto,
} from '../types/timetable'
import { authenticatedFetch } from '@/services/authService'

const API_BASE = 'http://localhost:8080/api/timetable'

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
 * Get available rooms for booking (T-102)
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

/**
 * Book a room for today by creating an override (T-104)
 * Teachers can book available rooms for immediate use
 */
export const bookRoomForToday = async (
  slotId: number,
  reason?: string
): Promise<OverrideDto> => {
  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format

  const request: OverrideRequest = {
    slotId,
    date: today,
    status: 'OCCUPIED',
    reason: reason || 'Booked for today',
  };

  const response = await authenticatedFetch(`${API_BASE}/override`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) throw new Error('Failed to book room')
  return response.json()
};
