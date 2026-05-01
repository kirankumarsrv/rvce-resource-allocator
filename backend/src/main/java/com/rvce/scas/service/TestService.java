package com.rvce.scas.service;

import com.rvce.scas.dto.TestResponseDto;
import org.springframework.stereotype.Service;

/**
 * Simple test service for verifying backend connectivity and availability.
 *
 * <p>This service provides a basic health check endpoint response. It is primarily used
 * during development and testing to verify that the Spring Boot backend is running
 * and responding to requests.
 *
 * <p>Example usage:
 * <pre>
 *   TestResponseDto response = testService.getMessage();
 *   // Returns a success response with message "Backend is working 🚀"
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see TestController for the REST endpoint that uses this service
 */
@Service
public class TestService {

    /**
     * Generates a test response message indicating the backend is operational.
     *
     * <p>This method creates a simple success response that can be used for health checks,
     * load balancer status verification, or basic connectivity testing.
     *
     * @return a {@link TestResponseDto} with success status and a test message
     */
    public TestResponseDto getMessage() {
        return new TestResponseDto(
                "Backend is working 🚀",
                "SUCCESS"
        );
    }
}
