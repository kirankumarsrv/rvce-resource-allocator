package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for a manually assigned exam seat.
 */
@Data
public class ExamSeatDto {

    private UUID seatId;
    private UUID examId;
    private UUID hallId;
    private UUID studentId;
    private String usn;
    private String studentName;
    private String branchCode;
    private boolean needsFrontRow;
    private int benchRow;
    private int benchCol;
    private int benchSeatIndex;
    private String benchNumber;
    private boolean manualOverride;
}