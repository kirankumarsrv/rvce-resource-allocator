package com.rvce.scas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for token refresh endpoint.
 *
 * <p><strong>Endpoint:</strong> POST /api/auth/refresh</p>
 *
 * <p><strong>Purpose:</strong> Obtains new access token without re-entering password.
 * Uses an opaque refresh token stored in Redis to validate the refresh request.
 * Supports rotating refresh tokens to limit replay attack surface.</p>
 *
 * <p><strong>Validation:</strong></p>
 * <ul>
 *   <li>{@code userId}: Must be a valid UUID (non-null)</li>
 *   <li>{@code refreshToken}: Must not be blank (typically a UUID string)</li>
 * </ul>
 *
 * <p><strong>Security Notes:</strong></p>
 * <ul>
 *   <li>Refresh token is sent in request body (not query parameter) to avoid logging</li>
 *   <li>Server validates that the refresh token exists in Redis and matches the user ID</li>
 *   <li>Old refresh token is deleted after new one is minted (token rotation)</li>
 *   <li>If validation fails, returns 401 Unauthorized</li>
 * </ul>
 *
 * <p><strong>Example Request:</strong></p>
 * <pre>
 *   POST /api/auth/refresh
 *   Content-Type: application/json
 *
 *   {
 *     "userId": "550e8400-e29b-41d4-a716-446655440000",
 *     "refreshToken": "550e8400-e29b-41d4-a716-446655440001"
 *   }
 * </pre>
 *
 * <p><strong>Response:</strong> HTTP 200 with {@link LoginResponse} containing
 * new access token and rotated refresh token.</p>
 *
 * @author RVCE SCAS Team
 * @see LoginResponse
 * @see AuthService#refresh(UUID, String)
 * @see TokenPair
 */
@Data
public class RefreshRequest {
    /**
     * UUID of the user requesting token refresh.
     * Must match the user ID associated with the refresh token in Redis.
     * Used to validate that the refresh token belongs to the requesting user.
     */
    @NotNull
    private UUID userId;

    /**
     * Opaque refresh token ID previously returned from login or prior refresh.
     * Typically a UUID string. Must exist in Redis and map to the provided userId.
     * Will be rotated (deleted) after new token is issued.
     */
    @NotBlank
    private String refreshToken;
}
