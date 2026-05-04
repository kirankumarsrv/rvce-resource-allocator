package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for teacher schedule information.
 * Represents a single time slot in a teacher's schedule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherScheduleDto {

    private Long slotId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String roomName;
    private String roomBuilding;
    private String subject;
    private String department;
    private Boolean isActive;

}
