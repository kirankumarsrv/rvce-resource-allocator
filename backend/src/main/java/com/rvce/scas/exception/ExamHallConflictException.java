package com.rvce.scas.exception;

/**
 * Thrown when a hall configuration conflicts with existing exam data.
 */
public class ExamHallConflictException extends RuntimeException {

    public ExamHallConflictException(String message) {
        super(message);
    }
}