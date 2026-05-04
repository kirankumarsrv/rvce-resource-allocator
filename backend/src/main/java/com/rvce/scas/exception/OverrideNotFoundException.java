package com.rvce.scas.exception;

/**
 * Exception thrown when trying to delete a non-existent override.
 * Maps to HTTP 404 Not Found.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
public class OverrideNotFoundException extends RuntimeException {

    public OverrideNotFoundException(String message) {
        super(message);
    }

}