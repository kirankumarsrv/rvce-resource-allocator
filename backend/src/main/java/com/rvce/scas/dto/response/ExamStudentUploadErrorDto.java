package com.rvce.scas.dto.response;

import lombok.Data;

/**
 * Row-level validation error returned from student CSV upload.
 */
@Data
public class ExamStudentUploadErrorDto {

    private int row;
    private String usn;
    private String error;
}
