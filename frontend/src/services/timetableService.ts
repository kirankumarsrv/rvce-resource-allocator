/**
 * API Service for T-105: Timetable Queries and Analytics
 */

import {
  TeacherScheduleDto,
  RoomScheduleDto,
  TimetableAnalyticsDto,
  TeacherWeeklySchedule,
  RoomWeeklySchedule,
} from '../types/timetable';

const API_BASE = 'http://localhost:8080/api/timetable';

/**
 * Get teacher's schedule for a specific day of week
 */
export const getTeacherSchedule = async (
  teacherId: string,
  dayOfWeek: number
): Promise<TeacherScheduleDto[]> => {
  const response = await fetch(
    `${API_BASE}/teacher/${teacherId}/schedule?dayOfWeek=${dayOfWeek}`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    }
  );
  if (!response.ok) throw new Error('Failed to fetch teacher schedule');
  return response.json();
};

/**
 * Get teacher's full weekly schedule (all 7 days)
 */
export const getTeacherWeeklySchedule = async (
  teacherId: string
): Promise<TeacherWeeklySchedule> => {
  const response = await fetch(
    `${API_BASE}/teacher/${teacherId}/schedule/weekly`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    }
  );
  if (!response.ok) throw new Error('Failed to fetch teacher weekly schedule');
  return response.json();
};

/**
 * Get room's schedule for a specific day of week
 */
export const getRoomSchedule = async (
  roomId: number,
  dayOfWeek: number
): Promise<RoomScheduleDto[]> => {
  const response = await fetch(
    `${API_BASE}/room/${roomId}/schedule?dayOfWeek=${dayOfWeek}`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    }
  );
  if (!response.ok) throw new Error('Failed to fetch room schedule');
  return response.json();
};

/**
 * Get room's full weekly schedule (all 7 days)
 */
export const getRoomWeeklySchedule = async (
  roomId: number
): Promise<RoomWeeklySchedule> => {
  const response = await fetch(
    `${API_BASE}/room/${roomId}/schedule/weekly`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    }
  );
  if (!response.ok) throw new Error('Failed to fetch room weekly schedule');
  return response.json();
};

/**
 * Get timetable analytics and utilization metrics
 */
export const getTimetableAnalytics = async (): Promise<TimetableAnalyticsDto> => {
  const response = await fetch(`${API_BASE}/analytics`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    },
  });
  if (!response.ok) throw new Error('Failed to fetch analytics');
  return response.json();
};
