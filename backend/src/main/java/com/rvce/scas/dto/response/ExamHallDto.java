package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for configured exam halls.
 */
@Data
public class ExamHallDto {

    private UUID hallId;
    private UUID examId;
    private UUID roomId;
    private String roomName;
    private String roomDisplayName;
    private Integer twoSeaterCount;
    private Integer threeSeaterCount;
    private Integer totalCapacity;
    private Integer benchRows;
    private Integer benchCols;
    private UUID invigilatorId;
    private String invigilatorName;
    private Integer sortOrder;
}