package com.rvce.scas.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an enrolled exam student.
 */
@Data
public class ExamStudentDto {

    private UUID entryId;
    private UUID examId;
    private UUID studentId;
    private String usn;
    private String studentName;
    private String branchCode;
    private String uploadBatchId;
    private Instant createdAt;
}
