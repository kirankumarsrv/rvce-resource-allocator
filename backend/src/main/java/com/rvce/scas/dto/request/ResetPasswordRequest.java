package com.rvce.scas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resetting a user's password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String reason; // Optional reason for password reset (e.g., "Forgot password", "Quarterly reset")
}
