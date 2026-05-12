package com.rvce.scas.service;

import com.rvce.scas.dto.request.ForgotPasswordRequest;
import com.rvce.scas.dto.request.ResetPasswordWithTokenRequest;
import com.rvce.scas.dto.response.ForgotPasswordResponseDto;
import com.rvce.scas.entity.PasswordResetToken;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.PasswordResetTokenRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Service for password reset flows (forgot password and reset-with-token).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final int TOKEN_LENGTH = 32;

    /**
     * Request a password reset for a user by email.
     * Generates a reset token and sends it via email.
     *
     * @param request forgot password request with email
     * @return response indicating token was sent (do not reveal if email exists)
     */
    @Transactional
    public ForgotPasswordResponseDto requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            // Always return success to avoid email enumeration
            return new ForgotPasswordResponseDto(
                    "If an account exists with that email, a password reset link will be sent."
            );
        }

        // Invalidate any existing tokens
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getUserId().equals(user.getUserId()) && !t.isUsed())
                .forEach(t -> {
                    t.setUsed(true);
                    t.setUsedAt(Instant.now());
                    tokenRepository.save(t);
                });

        // Generate new token
        String rawToken = generateRandomToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setUsed(false);
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));

        tokenRepository.save(resetToken);

        // Send email with reset link
        String resetLink = "http://localhost:3000/auth/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);

        log.info("Password reset token generated for user: {}", user.getEmail());

        return new ForgotPasswordResponseDto(
                "If an account exists with that email, a password reset link will be sent."
        );
    }

    /**
     * Reset password using a valid reset token.
     *
     * @param request token and new password
     * @return success/error message
     */
    @Transactional
    public Map<String, String> resetPasswordWithToken(ResetPasswordWithTokenRequest request) {
        String tokenHash = hashToken(request.getToken());

        PasswordResetToken resetToken = tokenRepository.findValidTokenByHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired password reset token attempted");
                    return new CsvValidationException("Invalid or expired password reset token");
                });

        User user = resetToken.getUser();

        // Update password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());

        return Map.of(
                "message", "Password has been reset successfully. You can now log in with your new password."
        );
    }

    /**
     * Generate a random token string.
     */
    private String generateRandomToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
