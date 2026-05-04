/**
 * Type definitions for T-105: Timetable Queries and Analytics
 */

export interface TeacherScheduleDto {
  slotId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  roomName: string;
  roomBuilding: string;
  subject: string;
  department: string;
  isActive: boolean;
}

export interface RoomScheduleDto {
  slotId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  teacherId: string;
  subject: string;
  department: string;
  isActive: boolean;
}

export interface TimetableAnalyticsDto {
  totalSlots: number;
  activeSlots: number;
  inactiveSlots: number;
  uniqueTeachers: number;
  uniqueRooms: number;
  dayOfWeekDistribution: Record<number, number>;
  morningSlots: number;
  afternoonSlots: number;
  eveningSlots: number;
  averageRoomCapacity: number;
  utilizationRate: number;
}

export interface TeacherWeeklySchedule {
  [dayOfWeek: number]: TeacherScheduleDto[];
}

export interface RoomWeeklySchedule {
  [dayOfWeek: number]: RoomScheduleDto[];
}

export interface WeeklySchedule {
  [dayOfWeek: number]: TeacherScheduleDto[] | RoomScheduleDto[];
}
