package com.rvce.scas.service;

import com.rvce.scas.dto.response.RoomScheduleDto;
import com.rvce.scas.dto.response.TeacherScheduleDto;
import com.rvce.scas.dto.response.TimetableAnalyticsDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.TimetableSlot;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.TimetableSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h3>Purpose</h3>
 * Service for timetable queries and analytics (T-105).
 * Provides schedule lookups, utilization metrics, and dashboard data.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Query teacher schedules</li>
 *   <li>Query room schedules</li>
 *   <li>Compute timetable analytics and utilization metrics</li>
 *   <li>Provide dashboard overview data</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on TimetableSlotRepository, RoomRepository.
 *
 * <h3>Transaction Behaviour</h3>
 * All methods are @Transactional(readOnly = true).
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableQueryService {

    private final TimetableSlotRepository slotRepository;
    private final RoomRepository roomRepository;

    /**
     * Retrieves the schedule for a specific teacher on a given day of week.
     * Returns all recurring slots for that day.
     *
     * <p>Decision DD-14: Schedule queries use day-of-week recurring model.</p>
     *
     * @param teacherId the teacher UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of teacher's scheduled slots for that day
     */
    @Transactional(readOnly = true)
    public List<TeacherScheduleDto> getTeacherSchedule(UUID teacherId, Integer dayOfWeek) {
        log.info("Fetching schedule for teacher {} on day {}", teacherId, dayOfWeek);

        List<TimetableSlot> slots = slotRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek);
        return slots.stream()
            .sorted(Comparator.comparing(TimetableSlot::getStartTime))
            .map(this::mapToTeacherScheduleDto)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves the full weekly schedule for a teacher (all 7 days).
     *
     * @param teacherId the teacher UUID
     * @return map of day-of-week to list of scheduled slots
     */
    @Transactional(readOnly = true)
    public Map<Integer, List<TeacherScheduleDto>> getTeacherWeeklySchedule(UUID teacherId) {
        log.info("Fetching weekly schedule for teacher {}", teacherId);

        Map<Integer, List<TeacherScheduleDto>> schedule = new java.util.LinkedHashMap<>();
        for (int day = 1; day <= 7; day++) {
            schedule.put(day, getTeacherSchedule(teacherId, day));
        }
        return schedule;
    }

    /**
     * Retrieves the schedule for a specific room on a given day of week.
     *
     * @param roomId the room ID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of slots scheduled in that room for that day
     */
    @Transactional(readOnly = true)
    public List<RoomScheduleDto> getRoomSchedule(Long roomId, Integer dayOfWeek) {
        log.info("Fetching schedule for room {} on day {}", roomId, dayOfWeek);

        List<TimetableSlot> slots = slotRepository.findByRoomIdAndDayOfWeek(roomId, dayOfWeek);
        return slots.stream()
            .sorted(Comparator.comparing(TimetableSlot::getStartTime))
            .map(this::mapToRoomScheduleDto)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves the full weekly schedule for a room (all 7 days).
     *
     * @param roomId the room ID
     * @return map of day-of-week to list of scheduled slots
     */
    @Transactional(readOnly = true)
    public Map<Integer, List<RoomScheduleDto>> getRoomWeeklySchedule(Long roomId) {
        log.info("Fetching weekly schedule for room {}", roomId);

        Map<Integer, List<RoomScheduleDto>> schedule = new java.util.LinkedHashMap<>();
        for (int day = 1; day <= 7; day++) {
            schedule.put(day, getRoomSchedule(roomId, day));
        }
        return schedule;
    }

    /**
     * Computes timetable analytics: utilization rates, slot distribution, capacity metrics.
     *
     * @return analytics DTO with summary metrics
     */
    @Transactional(readOnly = true)
    public TimetableAnalyticsDto getTimetableAnalytics() {
        log.info("Computing timetable analytics");

        long totalSlots = slotRepository.count();
        long activeSlots = slotRepository.countByIsActive(true);
        long uniqueTeachers = slotRepository.countDistinctTeachers();
        long uniqueRooms = slotRepository.countDistinctRooms();

        // Calculate day-of-week distribution
        Map<Integer, Long> dayDistribution = new java.util.LinkedHashMap<>();
        for (int day = 1; day <= 7; day++) {
            long count = slotRepository.countByDayOfWeek(day);
            dayDistribution.put(day, count);
        }

        // Calculate time slot utilization (morning, afternoon, evening)
        long morningSlots = slotRepository.countSlotsInTimeRange(
            LocalTime.of(6, 0), LocalTime.of(12, 0)
        );
        long afternoonSlots = slotRepository.countSlotsInTimeRange(
            LocalTime.of(12, 0), LocalTime.of(18, 0)
        );
        long eveningSlots = slotRepository.countSlotsInTimeRange(
            LocalTime.of(18, 0), LocalTime.of(23, 59)
        );

        // Calculate room capacity utilization
        List<Room> rooms = roomRepository.findAll();
        double avgCapacityUsed = rooms.isEmpty() ? 0.0 :
            (double) rooms.stream()
                .mapToLong(Room::getCapacity)
                .sum() / rooms.size();

        TimetableAnalyticsDto analytics = new TimetableAnalyticsDto();
        analytics.setTotalSlots(totalSlots);
        analytics.setActiveSlots(activeSlots);
        analytics.setInactiveSlots(totalSlots - activeSlots);
        analytics.setUniqueTeachers(uniqueTeachers);
        analytics.setUniqueRooms(uniqueRooms);
        analytics.setDayOfWeekDistribution(dayDistribution);
        analytics.setMorningSlots(morningSlots);
        analytics.setAfternoonSlots(afternoonSlots);
        analytics.setEveningSlots(eveningSlots);
        analytics.setAverageRoomCapacity(avgCapacityUsed);
        analytics.setUtilizationRate((double) activeSlots / Math.max(totalSlots, 1));

        log.debug("Analytics computed: {} total slots, {} unique teachers", totalSlots, uniqueTeachers);
        return analytics;
    }

    /**
     * Maps a TimetableSlot to a TeacherScheduleDto.
     */
    private TeacherScheduleDto mapToTeacherScheduleDto(TimetableSlot slot) {
        TeacherScheduleDto dto = new TeacherScheduleDto();
        dto.setSlotId(slot.getId());
        dto.setDayOfWeek(slot.getDayOfWeek());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setRoomName(slot.getRoom().getName());
        dto.setRoomBuilding(slot.getRoom().getBuilding());
        dto.setSubject(slot.getSubject());
        dto.setDepartment(slot.getDepartment());
        dto.setIsActive(slot.getIsActive());
        return dto;
    }

    /**
     * Maps a TimetableSlot to a RoomScheduleDto.
     */
    private RoomScheduleDto mapToRoomScheduleDto(TimetableSlot slot) {
        RoomScheduleDto dto = new RoomScheduleDto();
        dto.setSlotId(slot.getId());
        dto.setDayOfWeek(slot.getDayOfWeek());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setTeacherId(slot.getTeacher().getUserId());
        dto.setSubject(slot.getSubject());
        dto.setDepartment(slot.getDepartment());
        dto.setIsActive(slot.getIsActive());
        return dto;
    }

}
