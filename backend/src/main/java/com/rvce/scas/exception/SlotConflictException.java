package com.rvce.scas.exception;

/**
 * Exception thrown when a slot conflict is detected (e.g., duplicate slot or override).
 * Maps to HTTP 409 Conflict.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
public class SlotConflictException extends RuntimeException {

    public SlotConflictException(String message) {
        super(message);
    }

}