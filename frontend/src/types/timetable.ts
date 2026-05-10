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

/**
 * DTO for room availability queries (T-102).
 * Represents a room that is available for booking.
 */
export interface RoomAvailabilityDto {
  /** Unique identifier of the room */
  id: string;
  /** Human-readable name of the room (e.g., "LH-101") */
  name: string;
  /** Maximum capacity of the room */
  capacity: number;
  /** Building where the room is located */
  building: string;
  /** Floor number of the room */
  floor: number;
}

/**
 * Request parameters for room availability search
 */
export interface RoomAvailabilityQuery {
  /** Date to check availability (defaults to today if not provided) */
  date?: string;
  /** Start time of the time window */
  startTime: string;
  /** End time of the time window */
  endTime: string;
  /** Minimum capacity filter (optional) */
  minCapacity?: number;
  /** Building filter (optional) */
  building?: string;
}

/**
 * DTO for uploading timetable CSV file results.
 */
export interface UploadResultDto {
  /** Number of rows successfully imported */
  successCount: number;
  /** Number of rows that failed validation */
  errorCount: number;
  /** Optional list of validation or parsing errors. */
  errors?: string[];
  /** Optional summary message returned by the backend. */
  message?: string;
}

export interface SubstituteRequest {
  originalTeacherId: string;
  replacementTeacherId: string;
  startDate: string;
  endDate: string;
  scope: 'ONE_DAY' | 'SEMESTER';
}

export interface SubstitutionResultDto {
  reassignedCount: number;
  clashes: string[];
  message?: string;
}

/**
 * DTO for creating room booking overrides (T-104)
 */
export interface OverrideRequest {
  /** ID of the timetable slot to override */
  slotId: number;
  /** Date of the override */
  date: string;
  /** Status of the override (CANCELLED, CLAIMED, or EXTRA_CLASS) */
  status: 'CANCELLED' | 'CLAIMED' | 'EXTRA_CLASS';
  /** Optional reason for the override */
  reason?: string;
}

/**
 * Response DTO for override operations
 */
export interface OverrideDto {
  /** Unique identifier of the override */
  id: number;
  /** ID of the slot being overridden */
  slotId: number;
  /** Date of the override */
  date: string;
  /** Status of the override */
  status: 'CANCELLED' | 'CLAIMED' | 'EXTRA_CLASS';
  /** Reason for the override */
  reason?: string;
  /** User who created the override */
  createdBy: string;
  /** Timestamp when the override was created */
  createdAt: string;
}
