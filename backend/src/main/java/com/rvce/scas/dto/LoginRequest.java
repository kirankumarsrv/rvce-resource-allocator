package com.rvce.scas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for user login endpoint.
 *
 * <p><strong>Endpoint:</strong> POST /api/auth/login</p>
 *
 * <p><strong>Purpose:</strong> Captures user credentials for authentication.
 * Credentials are sent in request body (not URL query) to avoid logging and
 * exposure in browser history or Referer headers.</p>
 *
 * <p><strong>Validation:</strong></p>
 * <ul>
 *   <li>{@code email}: Must be a valid email format and not blank</li>
 *   <li>{@code password}: Must not be blank (length validation can be added server-side)</li>
 * </ul>
 *
 * <p><strong>Security Notes:</strong></p>
 * <ul>
 *   <li>Request should always be sent over HTTPS</li>
 *   <li>Server should never log the password field</li>
 *   <li>Client should send credentials only over HTTPS, never HTTP</li>
 *   <li>Email is normalized to lowercase on server side</li>
 * </ul>
 *
 * <p><strong>Example Request:</strong></p>
 * <pre>
 *   POST /api/auth/login
 *   Content-Type: application/json
 *
 *   {
 *     "email": "student@example.com",
 *     "password": "securePassword123"
 *   }
 * </pre>
 *
 * <p><strong>Response:</strong> HTTP 200 with {@link LoginResponse} containing
 * access token and refresh token.</p>
 *
 * @author RVCE SCAS Team
 * @see LoginResponse
 * @see AuthService#login(String, String)
 */
@Data
public class LoginRequest {
    /**
     * User's email address, used as the login identifier.
     * Must be a valid email format and not blank.
     * Will be normalized to lowercase for case-insensitive matching.
     */
    @NotBlank
    @Email
    private String email;

    /**
     * User's plaintext password.
     * Never stored or logged in plaintext. Compared against stored bcrypt hash.
     * Should be at least 8 characters long (validation can be added).
     */
    @NotBlank
    private String password;
}
