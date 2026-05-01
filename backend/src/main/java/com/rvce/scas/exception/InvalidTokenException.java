package com.rvce.scas.exception;

/**
 * Exception thrown when a JWT token or refresh token is invalid, expired, or missing.
 *
 * <p><strong>Causes:</strong></p>
 * <ul>
 *   <li>Refresh token does not exist in Redis (already revoked or expired)</li>
 *   <li>Refresh token belongs to a different user than the one requesting refresh</li>
 *   <li>User account is disabled (soft-deleted)</li>
 *   <li>User ID doesn't match the token claims</li>
 *   <li>Signature verification fails (token tampered with)</li>
 *   <li>Token has expired (beyond JWT expiry time)</li>
 *   <li>Token format is malformed</li>
 * </ul>
 *
 * <p><strong>Purpose:</strong> Signals that a token-based operation (refresh, validate) failed
 * and the client must re-authenticate to obtain new tokens.</p>
 *
 * <p><strong>Client Response:</strong> Returns HTTP 401 (Unauthorized). Client should clear
 * stored tokens and redirect user to login page.</p>
 *
 * <p><strong>Examples:</strong></p>
 * <pre>
 *   throw new InvalidTokenException("Refresh token is invalid or expired.");
 *   throw new InvalidTokenException("User account is disabled.");
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see AuthService#refresh(UUID, String)
 */
public class InvalidTokenException extends RuntimeException {
    /**
     * Constructs an InvalidTokenException with a detailed message.
     *
     * @param message human-readable message explaining why the token is invalid
     */
    public InvalidTokenException(String message) {
        super(message);
    }
}
