package com.rvce.scas.exception;

/**
 * Exception thrown when a referenced room does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String message) {
        super(message);
    }

}