package com.rvce.scas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple response DTO for backend health/connectivity checks.
 *
 * <p><strong>Endpoint:</strong> GET /api/test</p>
 *
 * <p><strong>Purpose:</strong> Verifies that the Spring Boot backend is running
 * and responding to requests. Used for health checks, load balancer status, and
 * development validation.</p>
 *
 * <p><strong>Field Descriptions:</strong></p>
 * <ul>
 *   <li>{@code message}: Human-readable status message (e.g., "Backend is working 🚀")</li>
 *   <li>{@code status}: Status indicator (typically "SUCCESS" for healthy response)</li>
 * </ul>
 *
 * <p><strong>Example Response:</strong></p>
 * <pre>
 *   HTTP 200 OK
 *   Content-Type: application/json
 *
 *   {
 *     "message": "Backend is working 🚀",
 *     "status": "SUCCESS"
 *   }
 * </pre>
 *
 * <p><strong>Typical Use Cases:</strong></p>
 * <ul>
 *   <li>Frontend initial load check</li>
 *   <li>Load balancer health probe</li>
 *   <li>Docker container startup check</li>
 *   <li>Development validation that server is up</li>
 * </ul>
 *
 * @author RVCE SCAS Team
 * @see TestService
 */
@Data
@AllArgsConstructor
public class TestResponseDto {
    /**
     * Informational message indicating backend status.
     * Non-null, human-readable string for display or logging.
     */
    private String message;

    /**
     * Status indicator for the health check.
     * Typically "SUCCESS" for successful response, "ERROR" for failures.
     */
    private String status;
}
