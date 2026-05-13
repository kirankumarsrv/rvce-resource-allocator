package com.rvce.scas.exception;

import com.rvce.scas.dto.response.ExamStudentUploadResultDto;

/**
 * Thrown when one or more student CSV rows fail validation.
 */
public class ExamStudentCsvValidationException extends RuntimeException {

    private final ExamStudentUploadResultDto result;

    public ExamStudentCsvValidationException(ExamStudentUploadResultDto result) {
        super("Exam student CSV validation failed");
        this.result = result;
    }

    public ExamStudentUploadResultDto getResult() {
        return result;
    }
}
