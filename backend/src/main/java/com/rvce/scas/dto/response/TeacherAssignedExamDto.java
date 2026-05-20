package com.rvce.scas.dto.response;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Teacher-facing view of an assigned exam as invigilator.
 */
@Data
public class TeacherAssignedExamDto {

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
    private String roomName;
}