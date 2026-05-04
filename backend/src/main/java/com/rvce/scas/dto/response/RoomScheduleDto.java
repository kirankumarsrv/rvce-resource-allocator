package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for room schedule information.
 * Represents a single time slot scheduled in a room.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomScheduleDto {

    private Long slotId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private UUID teacherId;
    private String subject;
    private String department;
    private Boolean isActive;

}
