package com.rvce.scas.controller;

import com.rvce.scas.dto.request.SubstituteRequest;
import com.rvce.scas.dto.response.*;
import com.rvce.scas.service.RoomAvailabilityService;
import com.rvce.scas.service.SubstitutionService;
import com.rvce.scas.service.TimetableQueryService;
import com.rvce.scas.service.TimetableUploadService;
import com.rvce.scas.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h3>Purpose</h3>
 * REST controller for Epic 1 timetable operations (T-101 to T-105).
 * Provides endpoints for upload, room availability, substitutions, and queries.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Handle HTTP requests and responses</li>
 *   <li>Enforce RBAC via @PreAuthorize</li>
 *   <li>Validate request parameters</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * Depends on: TimetableUploadService, RoomAvailabilityService, SubstitutionService, TimetableQueryService.
 *
 * <h3>Transaction Behaviour</h3>
 * Controllers are not transactional — services handle transactions.
 *
 * @author SCAS Engineering Team
 * @since 1.0
 */
@Tag(name = "Timetable", description = "Timetable management operations")
@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableUploadService uploadService;
    private final RoomAvailabilityService availabilityService;
    private final SubstitutionService substitutionService;
    private final TimetableQueryService queryService;
    private final UserRepository userRepository;

    /**
     * Uploads a CSV timetable file (T-101).
     * Requires TTO role. Parses, validates, and persists schedule data transactionally.
     *
     * @param file the CSV file to upload
     * @return upload result with success/error counts
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('TTO')")
    @Operation(summary = "Upload timetable CSV", description = "Parse and persist CSV timetable data")
    public ResponseEntity<UploadResultDto> uploadTimetable(@RequestParam("file") @NonNull MultipartFile file) {
        UploadResultDto result = uploadService.upload(file);
        return ResponseEntity.ok(result);
    }

    /**
     * Queries available rooms for information (T-102).
     * Returns cached results when available, otherwise queries database.
     * Read-only endpoint for checking room availability.
     *
     * @param date date to check (optional, defaults to today)
     * @param startTime start of time window
     * @param endTime end of time window
     * @param minCapacity minimum capacity filter
     * @param building building filter
     * @return list of available rooms
     */
    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Query rooms available during specified time")
    public ResponseEntity<List<RoomAvailabilityDto>> getAvailableRooms(
            @RequestParam(required = false) LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String building) {

        if (date == null) {
            date = LocalDate.now();
        }

        List<RoomAvailabilityDto> rooms = availabilityService.getAvailable(
            date, startTime, endTime, minCapacity, building
        );
        return ResponseEntity.ok(rooms);
    }



    /**
     * Lists teachers for substitution and scheduling lookups.
     *
     * @return teachers as lightweight id/text pairs
     */
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('TTO','ADMIN','SUPER_ADMIN','DEPT_COORD','EXAM_CONTROLLER')")
    @Operation(summary = "List teachers", description = "Retrieve teachers for dropdown selectors")
    public ResponseEntity<List<SimpleDto>> listTeachers() {
        List<SimpleDto> teachers = userRepository.findAllByRoleName("TEACHER").stream()
                .map(user -> new SimpleDto(user.getUserId().toString(), user.getName() + " (" + user.getEmail() + ")"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(teachers);
    }







    /**
     * Substitutes one teacher with another for a date range (T-103).
     * Requires TTO or ADMIN role. Performs clash detection and atomic reassignment.
     *
     * @param request substitution details
     * @return substitution result with reassignment count and clashes
     */
    @PostMapping("/substitute")
    @PreAuthorize("hasAnyRole('TTO','ADMIN')")
    @Operation(summary = "Substitute teacher", description = "Reassign teaching slots to another teacher")
    public ResponseEntity<SubstitutionResultDto> substituteTeacher(@Valid @RequestBody SubstituteRequest request) {
        SubstitutionResultDto result = substitutionService.substitute(request);
        return ResponseEntity.ok(result);
    }







    // ===== T-105: Timetable Queries and Analytics =====

    /**
     * Gets the schedule for a teacher on a specific day of week (T-105).
     *
     * @param teacherId the teacher UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of scheduled slots for that teacher on that day
     */
    @GetMapping("/teacher/{teacherId}/schedule")
    @PreAuthorize("hasAnyRole('TEACHER','TTO','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get teacher schedule", description = "Retrieve schedule for a teacher on a specific day")
    public ResponseEntity<List<TeacherScheduleDto>> getTeacherSchedule(
            @PathVariable UUID teacherId,
            @RequestParam Integer dayOfWeek) {

        List<TeacherScheduleDto> schedule = queryService.getTeacherSchedule(teacherId, dayOfWeek);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Gets the full weekly schedule for a teacher (T-105).
     *
     * @param teacherId the teacher UUID
     * @return map of day-of-week to scheduled slots
     */
    @GetMapping("/teacher/{teacherId}/schedule/weekly")
    @PreAuthorize("hasAnyRole('TEACHER','TTO','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get teacher weekly schedule", description = "Retrieve full week schedule for a teacher")
    public ResponseEntity<Map<Integer, List<TeacherScheduleDto>>> getTeacherWeeklySchedule(
            @PathVariable UUID teacherId) {

        Map<Integer, List<TeacherScheduleDto>> schedule = queryService.getTeacherWeeklySchedule(teacherId);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Gets the schedule for a room on a specific day of week (T-105).
     *
     * @param roomId the room ID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of scheduled slots in that room on that day
     */
    @GetMapping("/room/{roomId}/schedule")
    @Operation(summary = "Get room schedule", description = "Retrieve schedule for a room on a specific day")
    public ResponseEntity<List<RoomScheduleDto>> getRoomSchedule(
            @PathVariable Long roomId,
            @RequestParam Integer dayOfWeek) {

        List<RoomScheduleDto> schedule = queryService.getRoomSchedule(roomId, dayOfWeek);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Gets the full weekly schedule for a room (T-105).
     *
     * @param roomId the room ID
     * @return map of day-of-week to scheduled slots
     */
    @GetMapping("/room/{roomId}/schedule/weekly")
    @Operation(summary = "Get room weekly schedule", description = "Retrieve full week schedule for a room")
    public ResponseEntity<Map<Integer, List<RoomScheduleDto>>> getRoomWeeklySchedule(
            @PathVariable Long roomId) {

        Map<Integer, List<RoomScheduleDto>> schedule = queryService.getRoomWeeklySchedule(roomId);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Gets timetable analytics and utilization metrics (T-105).
     *
     * @return analytics DTO with summary statistics
     */
    @GetMapping("/analytics")
    @Operation(summary = "Get timetable analytics", description = "Retrieve timetable utilization and metrics")
    public ResponseEntity<TimetableAnalyticsDto> getTimetableAnalytics() {
        TimetableAnalyticsDto analytics = queryService.getTimetableAnalytics();
        return ResponseEntity.ok(analytics);
    }

}
