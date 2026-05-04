package com.rvce.scas.exception;

/**
 * Exception thrown when a referenced teacher does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
public class TeacherNotFoundException extends RuntimeException {

    public TeacherNotFoundException(String message) {
        super(message);
    }

}