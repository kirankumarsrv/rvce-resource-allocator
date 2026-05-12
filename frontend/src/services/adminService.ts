import type {
  TeacherListDto,
  CreateUserRequest,
  UserCreatedDto,
  ResetPasswordResponse,
  ResetPasswordRequest,
} from '../types/admin'
import { authenticatedFetch } from './authService'

const API_BASE = '/api/admin'

/**
 * List all teachers for invigilator assignment dropdown.
 */
export async function listTeachers(): Promise<TeacherListDto[]> {
  const response = await authenticatedFetch(`${API_BASE}/teachers`, {
    method: 'GET',
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Failed to load teachers' }))
    throw new Error(error.message || 'Failed to load teachers')
  }

  return response.json()
}

/**
 * Create a new user (teacher or student).
 */
export async function createUser(request: CreateUserRequest): Promise<UserCreatedDto> {
  const response = await authenticatedFetch(`${API_BASE}/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Failed to create user' }))
    throw new Error(error.message || 'Failed to create user')
  }

  return response.json()
}

/**
 * Reset a user's password.
 */
export async function resetPassword(
  userId: string,
  request: ResetPasswordRequest
): Promise<ResetPasswordResponse> {
  const response = await authenticatedFetch(`${API_BASE}/users/${userId}/reset-password`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Failed to reset password' }))
    throw new Error(error.message || 'Failed to reset password')
  }

  return response.json()
}
