package com.rvce.scas.controller;

import com.rvce.scas.dto.request.ForgotPasswordRequest;
import com.rvce.scas.dto.request.ResetPasswordWithTokenRequest;
import com.rvce.scas.dto.response.ForgotPasswordResponseDto;
import com.rvce.scas.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoints for password reset flows.
 * No authentication required (users can reset forgotten passwords).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Request a password reset (forgot password flow).
     * Sends a reset email with a token link if the account exists.
     *
     * @param request forgot password request with email
     * @return HTTP 200 with generic message (does not reveal if email exists)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponseDto response = passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password using a valid reset token.
     * Called after user receives reset email and clicks the link.
     *
     * @param request token and new password
     * @return HTTP 200 with success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordWithTokenRequest request) {
        Map<String, String> response = passwordResetService.resetPasswordWithToken(request);
        return ResponseEntity.ok(response);
    }
}
