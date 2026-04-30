package com.rvce.scas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for auditing and logging authentication and authorization events.
 *
 * <p>This service provides centralized logging of security-related events including:
 * <ul>
 *   <li>User login attempts (successful and failed)</li>
 *   <li>User logout events</li>
 * </ul>
 *
 * <p>All audit logs are written to the application logger with the prefix "AUDIT" for
 * easy filtering and monitoring. This enables security teams to track user access patterns,
 * detect anomalies, and maintain compliance audit trails.
 *
 * <p>Example usage:
 * <pre>
 *   auditService.logLogin(userId, email, true, null);
 *   auditService.logLogout(userId);
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see AuthService for authentication logic
 */
@Slf4j
@Service
public class AuditService {

    /**
     * Logs a login attempt with success/failure status and optional failure reason.
     *
     * <p>This method records both successful and failed login attempts for security auditing.
     * The user ID may be null for failed login attempts where the user could not be identified.
     *
     * @param userId the UUID of the user attempting to login, or {@code null} if login failed before user identification
     * @param email the email address of the user attempting to login
     * @param success {@code true} if the login was successful, {@code false} otherwise
     * @param reason optional reason for failure (e.g., "INVALID_CREDENTIALS", "ACCOUNT_LOCKED", "USER_NOT_FOUND")
     *               should be {@code null} for successful logins
     */
    public void logLogin(UUID userId, String email, boolean success, String reason) {
        log.info("AUDIT login userId={} email={} success={} reason={}", userId, email, success, reason);
    }

    /**
     * Logs a user logout event.
     *
     * <p>This method records when a user explicitly logs out from the system.
     * It is used in conjunction with token blacklisting to ensure audit compliance.
     *
     * @param userId the UUID of the user performing the logout
     */
    public void logLogout(UUID userId) {
        log.info("AUDIT logout userId={}", userId);
    }
}
