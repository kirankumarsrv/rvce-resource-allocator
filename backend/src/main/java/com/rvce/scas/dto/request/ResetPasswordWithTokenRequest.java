package com.rvce.scas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resetting password with a valid reset token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordWithTokenRequest {
    private String token; // Reset token from email link
    private String newPassword; // New password to set
}
