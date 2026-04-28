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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(base(request, 400, "Bad Request", "CONSTRAINT_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(request, 401, "Unauthorized", "INVALID_CREDENTIALS", "Invalid email or password."));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseDto> handleLocked(
            AccountLockedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(request, 401, "Unauthorized", "ACCOUNT_LOCKED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(base(request, 401, "Unauthorized", "INVALID_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(SlotAlreadyClaimedException.class)
    public ResponseEntity<ErrorResponseDto> handleSlotClaimed(
            SlotAlreadyClaimedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(request, 409, "Conflict", "SLOT_ALREADY_CLAIMED", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDbConstraint(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(request, 409, "Conflict", "DATA_INTEGRITY_VIOLATION", "Request conflicts with existing data."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(base(request, 403, "Forbidden", "INSUFFICIENT_PERMISSIONS", "You do not have permission to perform this action."));
    }

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
