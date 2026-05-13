import type {
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ResetPasswordWithTokenRequest,
  ResetPasswordResponse,
} from '@/types/passwordReset'

const API_BASE = '/api/auth'

/**
 * Request a password reset (forgot password flow).
 * Sends a reset email if the account exists.
 */
export async function requestPasswordReset(
  request: ForgotPasswordRequest
): Promise<ForgotPasswordResponse> {
  const response = await fetch(`${API_BASE}/forgot-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    let errorMessage = 'Failed to process password reset request'
    try {
      const errorData = await response.json()
      if (errorData.message) {
        errorMessage = errorData.message
      }
    } catch {
      // Keep default message
    }
    throw new Error(errorMessage)
  }

  return response.json()
}

/**
 * Reset password using a valid token.
 */
export async function resetPasswordWithToken(
  request: ResetPasswordWithTokenRequest
): Promise<ResetPasswordResponse> {
  const response = await fetch(`${API_BASE}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    let errorMessage = 'Failed to reset password'
    try {
      const errorData = await response.json()
      if (errorData.message) {
        errorMessage = errorData.message
      }
    } catch {
      // Keep default message
    }
    throw new Error(errorMessage)
  }

  return response.json()
}
