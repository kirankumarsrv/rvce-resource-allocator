package com.rvce.scas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned from successful login endpoint.
 *
 * <p><strong>Endpoint:</strong> POST /api/auth/login</p>
 *
 * <p><strong>Purpose:</strong> Returns authentication tokens and metadata to the client
 * after successful credential validation.</p>
 *
 * <p><strong>Field Descriptions:</strong></p>
 * <ul>
 *   <li>{@code accessToken}: RS256-signed JWT for API authorization (15-minute expiry)</li>
 *   <li>{@code refreshToken}: Opaque identifier for token refresh (7-day expiry)</li>
 *   <li>{@code tokenType}: Always "Bearer" - indicates HTTP Authorization header format</li>
 *   <li>{@code expiresIn}: Access token lifetime in seconds (900 = 15 minutes)</li>
 * </ul>
 *
 * <p><strong>Client Usage:</strong></p>
 * <ol>
 *   <li>Extract accessToken and store securely (memory or secure cookie)</li>
 *   <li>Extract refreshToken and store in secure storage</li>
 *   <li>Use token format: {@code Authorization: Bearer <accessToken>}</li>
 *   <li>When expiresIn time has elapsed, call /api/auth/refresh to get new tokens</li>
 * </ol>
 *
 * <p><strong>Example Response:</strong></p>
 * <pre>
 *   HTTP 200 OK
 *   Content-Type: application/json
 *
 *   {
 *     "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *     "tokenType": "Bearer",
 *     "expiresIn": 900
 *   }
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see LoginRequest
 * @see TokenPair
 * @see AuthService#login(String, String)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /**
     * RS256-signed JWT access token.
     * Contains user ID, email, roles, and fine-grained permissions.
     * Signed with server's private key and verified with public key.
     */
    private String accessToken;

    /**
     * Opaque refresh token identifier.
     * Typically a UUID stored in Redis with associatedi user ID.
     * Used to obtain new access tokens without password.
     */
    private String refreshToken;

    /**
     * Token authentication scheme.
     * Always "Bearer" to indicate format is: Authorization: Bearer <token>
     */
    private String tokenType;

    /**
     * Access token lifetime in seconds.
     * Indicates to client when to call refresh endpoint (typically 900 = 15 minutes).
     */
    private int expiresIn;
}
