package com.rvce.scas.dto.response;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Student-facing view of a published exam, including optional seat details.
 */
@Data
public class StudentPublishedExamDto {

    private UUID examId;
    private String examName;
    private String subjectCode;
    private String subjectName;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Instant publishedAt;

    private UUID hallId;
    private String hallName;
    private String benchNumber;
    private Integer benchRow;
    private Integer benchCol;
    private Integer benchSeatIndex;
}