/**
 * Type definitions for exam seating (T-401 through T-405)
 */

export interface ExamSessionDto {
  examId: string
  name: string
  subjectCode: string
  section: string | null
  semester: number
  departmentId: string
  departmentName: string
  examDate: string
  startTime: string
  endTime: string
  status: 'DRAFT' | 'CONFIGURED' | 'GENERATED' | 'PUBLISHED' | 'COMPLETED' | 'CANCELLED'
  createdAt: string
  updatedAt: string
  studentCount?: number
}

export interface CreateExamSessionRequest {
  name: string
  subjectCode: string
  subjectName: string
  section: string | null
  semester: number
  examDate: string
  startTime: string
  endTime: string
}

export interface ExamHallDto {
  hallId: string
  examId: string
  roomId: string
  roomName: string
  roomDisplayName: string
  twoSeaterCount: number
  threeSeaterCount: number
  totalCapacity: number
  benchRows: number
  benchCols: number
  invigilatorId: string | null
  sortOrder?: number
}

export interface HallGridCellDto {
  row: number
  col: number
  label: string
  seatCapacity: number
  occupiedCount: number
  active: boolean
  excluded: boolean
  seats: SeatDto[]
  warnings: SeatWarningDto[]
}

export interface HallGridDto {
  hallId: string
  roomName: string
  roomDisplayName: string
  benchRows: number
  benchCols: number
  grid: HallGridCellDto[][]
}

export interface SeatWarningDto {
  type: 'SAME_BRANCH_BENCH' | 'MIDDLE_SEAT_EMPTY' | 'OVER_CAPACITY' | string
  benchNumber?: string
  hallId?: string
  message: string
  detail?: string
}

export interface StudentSeatAssignmentDto {
  examId: string
  examName: string
  subjectCode: string
  subjectName: string
  examDate: string
  startTime: string
  endTime: string
  status: string
  publishedAt?: string | null
  hallId: string
  hallName: string
  benchNumber: string
  benchRow: number
  benchCol: number
  benchSeatIndex: number
}

export interface StudentPublishedExamDto {
  examId: string
  examName: string
  subjectCode: string
  subjectName: string
  examDate: string
  startTime: string
  endTime: string
  status: string
  publishedAt?: string | null
  hallId?: string | null
  hallName?: string | null
  benchNumber?: string | null
  benchRow?: number | null
  benchCol?: number | null
  benchSeatIndex?: number | null
}

export interface ExamSeatDto {
  seatId: string
  examId: string
  hallId: string
  studentId: string | null
  benchRow: number
  benchCol: number
  benchSeatIndex: number
  benchNumber: string
  manualOverride: boolean
  usn?: string | null
  studentName?: string | null
  branchCode?: string | null
}

export interface SeatDto {
  seatId: string
  examId: string
  studentId: string | null
  hallId: string
  benchRow: number
  benchCol: number
  benchSeatIndex: number // 0, 1, or 2
  benchNumber: string
  usn: string | null
  studentName: string | null
  branchCode: string | null
  isManualOverride: boolean
}

export interface BenchCellDto {
  row: number
  col: number
  benchNumber: string
  benchType: 2 | 3
  seats: SeatDto[]
  occupancyCount: number
  excluded: boolean
}

export interface UnassignedStudentDto {
  entryId: string
  studentId: string | null
  usn: string
  studentName: string
  branchCode: string
  studentClass?: string
  section?: string
  departmentName?: string
  reason?: 'NO_USER_ACCOUNT' | 'UNASSIGNED'
}

export interface StudentGroupDto {
  groupId: string
  label: string
  students: UnassignedStudentDto[]
}

export interface SeatingDashboardStateDto {
  examId: string
  sessionId: string
  halls: ExamHallDto[]
  hallGrids: HallGridDto[]
  assignedSeats: ExamSeatDto[]
  unassignedStudents: UnassignedStudentDto[]
  assignedCount: number
  totalCount: number
}

export interface SeatingSessionDto {
  sessionId: string
  examId: string
  openedAt: string
  coordinatorId: string
}

export interface SeatPlacementRequest {
  studentId: string
  hallId: string
  benchRow: number
  benchCol: number
  benchSeatIndex: number
}

export interface BulkSeatSaveRequest {
  assignments: SeatPlacementRequest[]
}

export interface ClearHallRequest {
  examId: string
  hallId: string
}

export interface ExamHallConfigRequest {
  roomId: string
  twoSeaterCount: number
  threeSeaterCount: number
  invigilatorId: string  // REQUIRED: Must select an invigilator (UUID)
}

export interface RoomDto {
  id: string
  name: string
  block: string
  floor: number
  capacity: number
}
