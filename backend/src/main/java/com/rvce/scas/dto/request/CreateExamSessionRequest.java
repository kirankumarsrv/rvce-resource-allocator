package com.rvce.scas.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request payload for creating an exam session.
 */
@Data
public class CreateExamSessionRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String subjectCode;

    @NotBlank
    @Size(max = 100)
    private String subjectName;

    @Size(max = 10)
    private String section;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer semester;

    private UUID departmentId;

    @Size(max = 100)
    private String departmentName;

    @NotNull
    private LocalDate examDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
