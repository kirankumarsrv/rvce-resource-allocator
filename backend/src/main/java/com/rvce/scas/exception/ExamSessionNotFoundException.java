package com.rvce.scas.exception;

/**
 * Thrown when an exam session cannot be found.
 */
public class ExamSessionNotFoundException extends RuntimeException {

    public ExamSessionNotFoundException(String message) {
        super(message);
    }
}
