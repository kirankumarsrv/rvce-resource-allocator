package com.rvce.scas.exception;

/**
 * Exception thrown when no pre-seeded directions exist for a room/start-point pair.
 * Maps to HTTP 404 Not Found.
 */
public class DirectionsNotFoundException extends RuntimeException {

    public DirectionsNotFoundException(String message) {
        super(message);
    }

}