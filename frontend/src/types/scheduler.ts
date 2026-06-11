// ─── Request types ────────────────────────────────────────────────────────────

export type SubjectType = 'THEORY' | 'LAB'
export type RoomType = 'CLASSROOM' | 'LAB'
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
export type TimeSlotKey =
  | 'SLOT_9AM'
  | 'SLOT_10AM'
  | 'SLOT_1130AM'
  | 'SLOT_1230PM'
  | 'SLOT_230PM'
  | 'SLOT_330PM'

export interface SchedulerSubject {
  id: string
  name: string
  department: string
  year: number
  section: string
  batch?: string
  credits: number
  type: SubjectType
  teacherId: string
  labTeacherId?: string
  fixedRoomId?: string
  isElective?: boolean
  electiveSlot?: string
  semester: string
  requiredLabType?: string
  theoryOnlyFourCredit?: boolean
  theoryHoursPerWeek?: number
  labHoursPerWeek?: number
}

export interface SchedulerRoom {
  id: string
  name: string
  type: RoomType
  capacity: number
  labType?: string
}

export interface DepartmentInput {
  department: string
  subjects: SchedulerSubject[]
  rooms: SchedulerRoom[]
  daysInWeek: number
}

// ─── Response types ───────────────────────────────────────────────────────────

export interface ScheduledSlotSubject {
  id: string
  name: string
  department: string
  year: string
  section: string
  batch?: string
  credits: number
  type: SubjectType
  teacherId: string
  labTeacherId?: string
  semester: string
}

export interface ScheduledSlotRoom {
  id: string
  name: string
  type: RoomType
  capacity: number
  labType?: string
}

export interface ScheduledSlot {
  subject: ScheduledSlotSubject
  room: ScheduledSlotRoom
  day: DayOfWeek
  timeSlot: TimeSlotKey
  isLabSecondHour: boolean
}

export interface TeacherLoadEntry {
  teacherId: string
  totalHours: number
}

export interface SchedulerResult {
  scheduledSlots: ScheduledSlot[]
  rooms: ScheduledSlotRoom[]
  daysInWeek: number
  unscheduledHours: Record<string, number>
  teacherLoadSummary: Record<string, number>
  isFullyScheduled: boolean
}

// ─── UI-only helpers ──────────────────────────────────────────────────────────

export const TIME_SLOT_LABELS: Record<TimeSlotKey, string> = {
  SLOT_9AM: '9:00 AM',
  SLOT_10AM: '10:00 AM',
  SLOT_1130AM: '11:30 AM',
  SLOT_1230PM: '12:30 PM',
  SLOT_230PM: '2:30 PM',
  SLOT_330PM: '3:30 PM',
}

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
}

export const ALL_DAYS: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]

export const ALL_TIME_SLOTS: TimeSlotKey[] = [
  'SLOT_9AM',
  'SLOT_10AM',
  'SLOT_1130AM',
  'SLOT_1230PM',
  'SLOT_230PM',
  'SLOT_330PM',
]

export const newSubject = (): SchedulerSubject => ({
  id: '',
  name: '',
  department: '',
  year: 3,
  section: '',
  batch: '',
  credits: 3,
  type: 'THEORY',
  teacherId: '',
  semester: '',
})

export const newRoom = (): SchedulerRoom => ({
  id: '',
  name: '',
  type: 'CLASSROOM',
  capacity: 60,
})
