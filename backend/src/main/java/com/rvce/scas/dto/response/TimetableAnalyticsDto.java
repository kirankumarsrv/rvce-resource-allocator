package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for timetable analytics and overview metrics.
 * Provides summary statistics about the current timetable state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableAnalyticsDto {

    private Long totalSlots;
    private Long activeSlots;
    private Long inactiveSlots;
    private Long uniqueTeachers;
    private Long uniqueRooms;
    
    // Day of week distribution (1=Monday through 7=Sunday)
    private Map<Integer, Long> dayOfWeekDistribution;
    
    // Time slot distribution
    private Long morningSlots;    // 6:00 - 12:00
    private Long afternoonSlots;  // 12:00 - 18:00
    private Long eveningSlots;    // 18:00 - 23:59
    
    // Utilization metrics
    private Double averageRoomCapacity;
    private Double utilizationRate;  // activeSlots / totalSlots

}
