export interface TeacherListDto {
  userId: string
  name: string
  email: string
  department: string
}

export interface CreateUserRequest {
  name: string
  email: string
  usn?: string | null
  role: string
  departmentCode: string
}

export interface UserCreatedDto {
  userId: string
  email: string
  name: string
  tempPassword: string
  role: string
  department: string
}

export interface ResetPasswordRequest {
  reason?: string
}

export interface ResetPasswordResponse {
  email: string
  tempPassword: string
  message: string
}
