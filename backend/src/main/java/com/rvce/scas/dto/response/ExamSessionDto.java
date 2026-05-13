package com.rvce.scas.dto.response;

import com.rvce.scas.entity.ExamSession;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for exam session details.
 */
@Data
public class ExamSessionDto {

    private UUID examId;
    private String name;
    private String subjectCode;
    private String subjectName;
    private String section;
    private Integer semester;
    private UUID departmentId;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ExamSession.ExamStatus status;
    private UUID createdBy;
    private Instant publishedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private long studentCount;
}
