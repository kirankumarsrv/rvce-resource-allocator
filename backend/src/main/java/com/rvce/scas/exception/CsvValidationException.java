package com.rvce.scas.exception;

/**
 * Exception thrown when CSV validation fails during upload (T-101).
 * Contains details about which rows failed and why.
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
public class CsvValidationException extends RuntimeException {

    public CsvValidationException(String message) {
        super(message);
    }

}