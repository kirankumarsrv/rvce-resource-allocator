package com.rvce.scas.exception;

/**
 * Thrown when an exam hall cannot be found.
 */
public class ExamHallNotFoundException extends RuntimeException {

    public ExamHallNotFoundException(String message) {
        super(message);
    }
}