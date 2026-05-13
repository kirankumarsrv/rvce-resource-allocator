package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for password reset request (forgot password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponseDto {
    private String message; // Status message (generic to avoid email enumeration)
}
