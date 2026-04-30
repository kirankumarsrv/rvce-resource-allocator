package com.rvce.scas.exception;

/**
 * Exception thrown when a user account is locked due to too many failed login attempts.
 *
 * <p><strong>When it occurs:</strong> After 5 consecutive failed login attempts within a
 * 15-minute window, the account is locked and this exception is thrown on subsequent
 * login attempts for that email.</p>
 *
 * <p><strong>Purpose:</strong> Prevents brute-force password guessing attacks by temporarily
 * disabling the account. The lockout is time-based and automatically expires after 15 minutes.</p>
 *
 * <p><strong>Lockout Mechanism:</strong> Distributed across all instances via Redis.
 * Redis key: {@code login:locked:email@example.com} with TTL of 15 minutes.</p>
 *
 * <p><strong>Client Response:</strong> Returns HTTP 401 (Unauthorized) with this exception's
 * message, instructing the user to retry after the lockout period expires.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 *   throw new AccountLockedException("Account locked. Try again in 15 minute(s).");
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see AuthService#login(String, String)
 * @see AuthService#checkLockout(String)
 * @see AuthService#lockAccount(String)
 */
public class AccountLockedException extends RuntimeException {
    /**
     * Constructs an AccountLockedException with a detailed message.
     *
     * @param message human-readable message explaining the lockout duration,
     *                e.g., "Account locked. Try again in 15 minute(s)."
     */
    public AccountLockedException(String message) {
        super(message);
    }
}
