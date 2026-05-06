package com.rvce.scas.hardening;

import com.rvce.scas.dto.ErrorResponseDto;
import com.rvce.scas.exception.AccountLockedException;
import com.rvce.scas.exception.InvalidTokenException;
import com.rvce.scas.exception.SlotAlreadyClaimedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts application exceptions into structured JSON error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles bean validation failures on request bodies.
         *
         * @param ex the validation exception raised by Spring MVC
         * @param request the current HTTP request
         * @return a 400 response containing per-field validation messages
         */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Bad Request")
                .code("VALIDATION_FAILED")
                .message("Validation failed for one or more fields.")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build());
    }

        /**
         * Handles constraint violations raised from method or parameter validation.
         *
         * @param ex the constraint violation exception
         * @param request the current HTTP request
         * @return a 400 response with the validation message
         */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(base(request, 400, "Bad Request", "CONSTRAINT_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(base(
                request,
                400,
                "Bad Request",
                "MISSING_REQUEST_PARAMETER",
                String.format("Required request parameter '%s' is missing.", ex.getParameterName())
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(base(
                request,
                400,
                "Bad Request",
                "INVALID_REQUEST_PARAMETER",
                String.format("Request parameter '%s' has invalid value '%s'. Expected type: %s.",
                        ex.getName(),
                        ex.getValue(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
                )
        ));
    }

        /**
         * Handles invalid login credentials.
         *
         * @param ex the authentication failure exception
         * @param request the current HTTP request
         * @return a 401 response with a generic authentication error
         */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(request, 401, "Unauthorized", "INVALID_CREDENTIALS", "Invalid email or password."));
    }

        /**
         * Handles account lockout responses after repeated failures.
         *
         * @param ex the account lockout exception
         * @param request the current HTTP request
         * @return a 429 response indicating that the account is temporarily locked
         */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseDto> handleLocked(
            AccountLockedException ex, HttpServletRequest request) {
        // FIX: return 429 (TOO_MANY_REQUESTS) instead of 401 for rate-limit/brute-force protection.
        // Clients can distinguish between invalid credentials (401) and rate-limit (429).
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(base(request, 429, "Too Many Requests", "ACCOUNT_LOCKED", ex.getMessage()));
    }

    /**
     * Handles expired or malformed refresh tokens.
     *
     * @param ex the invalid token exception
     * @param request the current HTTP request
     * @return a 401 response with the token failure details
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(request, 401, "Unauthorized", "INVALID_TOKEN", ex.getMessage()));
    }

    /**
     * Handles conflicts when a slot has already been claimed.
     *
     * @param ex the slot collision exception
     * @param request the current HTTP request
     * @return a 409 response describing the conflict
     */
    @ExceptionHandler(SlotAlreadyClaimedException.class)
    public ResponseEntity<ErrorResponseDto> handleSlotClaimed(
            SlotAlreadyClaimedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(request, 409, "Conflict", "SLOT_ALREADY_CLAIMED", ex.getMessage()));
    }

    /**
     * Handles database-level uniqueness and foreign-key violations.
     *
     * @param ex the persistence exception
     * @param request the current HTTP request
     * @return a 409 response describing the data conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDbConstraint(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(request, 409, "Conflict", "DATA_INTEGRITY_VIOLATION", "Request conflicts with existing data."));
    }

    /**
     * Handles authorization failures after authentication succeeds.
     *
     * @param ex the access denied exception
     * @param request the current HTTP request
     * @return a 403 response describing the missing permissions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(base(request, 403, "Forbidden", "INSUFFICIENT_PERMISSIONS", "You do not have permission to perform this action."));
    }

    /**
     * Handles business logic validation errors (e.g., missing required parameters, invalid time ranges).
     *
     * @param ex the illegal argument exception
     * @param request the current HTTP request
     * @return a 400 response with the validation error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(base(request, 400, "Bad Request", "INVALID_REQUEST", ex.getMessage()));
    }

    /**
     * Handles unexpected failures and returns a supportable incident id.
     *
     * @param ex the unexpected exception
     * @param request the current HTTP request
     * @return a 500 response with an incident reference
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        String incidentId = UUID.randomUUID().toString();
        log.error("Unhandled exception incidentId={}", incidentId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(base(
                        request,
                        500,
                        "Internal Server Error",
                        "UNEXPECTED_ERROR",
                        "An unexpected error occurred. Incident ID: " + incidentId
                ));
    }

        /**
         * Builds a standard error payload shared by all handlers.
         *
         * @param request the current HTTP request
         * @param status the HTTP status code to report
         * @param error the short error label
         * @param code the machine-readable application error code
         * @param message the human-readable error message
         * @return a populated error response DTO
         */
    private ErrorResponseDto base(HttpServletRequest request, int status, String error, String code, String message) {
        return ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
