package com.rvce.scas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data transfer object containing a pair of JWT tokens returned from authentication endpoints.
 *
 * <p><strong>Contents:</strong></p>
 * <ul>
 *   <li>{@code accessToken}: Short-lived JWT (15 minutes) containing user identity and permissions.</li>
 *   <li>{@code refreshToken}: Opaque identifier (UUID string) for obtaining new access tokens.</li>
 * </ul>
 *
 * <p><strong>Usage Flow:</strong></p>
 * <ol>
 *   <li>Client calls /api/auth/login with email/password</li>
 *   <li>Server returns TokenPair with accessToken + refreshToken</li>
 *   <li>Client stores both tokens securely (accessToken in memory/secure cookie, refreshToken
 *       in secure storage)</li>
 *   <li>Client includes accessToken in Authorization header for API requests</li>
 *   <li>When accessToken expires, client calls /api/auth/refresh with userId + refreshToken</li>
 *   <li>Server returns new TokenPair with rotated refreshToken</li>
 * </ol>
 *
 * <p><strong>Security Notes:</strong></p>
 * <ul>
 *   <li>Access token is a signed JWT visible to the client (contains claims but is signature-protected)</li>
 *   <li>Refresh token is opaque - only the server knows its meaning (stored in Redis)</li>
 *   <li>Refresh tokens should be stored in secure storage (secure cookies, encrypted local storage)</li>
 *   <li>Never store refresh tokens in localStorage if XSS risk exists</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see AuthService#login(String, String)
 * @see AuthService#refresh(java.util.UUID, String)
 */
@Data
@AllArgsConstructor
public class TokenPair {
    /**
     * RS256-signed JWT access token with 15-minute expiry.
     * Contains claims: sub (user ID), email, authorities (roles + permissions).
     * Sent in Authorization: Bearer header for API requests.
     */
    private String accessToken;

    /**
     * Opaque refresh token ID (typically a UUID stored in Redis).
     * Used to obtain a new access token without re-entering password.
     * Linked to user in Redis with a 7-day TTL.
     */
    private String refreshToken;
}
