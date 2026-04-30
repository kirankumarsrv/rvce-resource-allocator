package com.rvce.scas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response DTO used by GlobalExceptionHandler for all exceptions.
 *
 * <p><strong>Purpose:</strong> Provides consistent error response format across all
 * API endpoints. Used when any exception occurs during request processing.</p>
 *
 * <p><strong>Field Descriptions:</strong></p>
 * <ul>
 *   <li>{@code timestamp}: ISO 8601 timestamp when error occurred</li>
 *   <li>{@code status}: HTTP status code (e.g., 401, 403, 404, 500)</li>
 *   <li>{@code error}: HTTP error name (e.g., "Unauthorized", "Forbidden")</li>
 *   <li>{@code code}: Application-specific error code for client-side error handling</li>
 *   <li>{@code message}: Human-readable error description</li>
 *   <li>{@code path}: Request URI path that caused the error</li>
 *   <li>{@code fieldErrors}: Map of validation error messages (field name -> error message)</li>
 * </ul>
 *
 * <p><strong>Example Responses:</strong></p>
 * <pre>
 *   HTTP 401 Unauthorized
 *   {
 *     "timestamp": "2024-04-30T12:34:56Z",
 *     "status": 401,
 *     "error": "Unauthorized",
 *     "code": "INVALID_CREDENTIALS",
 *     "message": "Invalid email or password.",
 *     "path": "/api/auth/login",
 *     "fieldErrors": null
 *   }
 *
 *   HTTP 400 Bad Request (Validation Error)
 *   {
 *     "timestamp": "2024-04-30T12:34:56Z",
 *     "status": 400,
 *     "error": "Bad Request",
 *     "code": "VALIDATION_ERROR",
 *     "message": "Request validation failed",
 *     "path": "/api/auth/login",
 *     "fieldErrors": {
 *       "email": "must be a valid email address",
 *       "password": "must not be blank"
 *     }
 *   }
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see GlobalExceptionHandler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    /**
     * ISO 8601 timestamp when the error occurred.
     * Useful for debugging and logging error sequences.
     */
    private Instant timestamp;

    /**
     * HTTP status code of the error response.
     * Examples: 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 500 (Server Error).
     */
    private int status;

    /**
     * HTTP error name corresponding to the status code.
     * Examples: "Bad Request", "Unauthorized", "Forbidden", "Not Found", "Internal Server Error".
     */
    private String error;

    /**
     * Application-specific error code for programmatic handling.
     * Examples: "INVALID_CREDENTIALS", "ACCOUNT_LOCKED", "RESOURCE_NOT_FOUND".
     * Used by client-side code to determine user-facing error messages or retry logic.
     */
    private String code;

    /**
     * Human-readable error message describing what went wrong.
     * Should be suitable for display to end users (non-technical language).
     */
    private String message;

    /**
     * Request URI path that caused the error.
     * Useful for debugging and request tracing.
     */
    private String path;

    /**
     * Map of field validation errors (only for validation failures).
     * Maps field name to error message. {@code null} if not a validation error.
     * Example: {"email": "must be a valid email", "password": "must not be blank"}
     */
    private Map<String, String> fieldErrors;
}
