package com.rvce.scas.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Complete dashboard state for manual seating.
 */
@Data
public class SeatingDashboardStateDto {

    private UUID examId;
    private UUID sessionId;
    private List<ExamHallDto> halls;
    private List<HallGridDto> hallGrids;
    private List<ExamSeatDto> assignedSeats;
    private List<UnassignedStudentDto> unassignedStudents;
    private int assignedCount;
    private int totalCount;
}