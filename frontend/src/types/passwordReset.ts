export interface ForgotPasswordRequest {
  email: string
}

export interface ForgotPasswordResponse {
  message: string
}

export interface ResetPasswordWithTokenRequest {
  token: string
  newPassword: string
}

export interface ResetPasswordResponse {
  message: string
}
