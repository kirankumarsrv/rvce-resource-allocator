package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.UUID;

/**
 * Student enrolled in an exam but not yet seated.
 */
@Data
public class UnassignedStudentDto {

    private UUID entryId;
    private UUID studentId;
    private String usn;
    private String studentName;
    private String branchCode;
    private String reason;
}