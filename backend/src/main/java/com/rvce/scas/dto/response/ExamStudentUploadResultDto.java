package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for exam student CSV uploads.
 */
@Data
public class ExamStudentUploadResultDto {

    private int totalRows;
    private int inserted;
    private int skipped;
    private List<ExamStudentUploadErrorDto> errors;
}
