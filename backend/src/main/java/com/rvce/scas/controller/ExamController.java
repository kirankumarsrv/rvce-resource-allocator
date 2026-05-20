package com.rvce.scas.controller;

import com.rvce.scas.dto.request.BulkSeatSaveRequest;
import com.rvce.scas.dto.request.CreateExamSessionRequest;
import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.dto.response.ExamStudentUploadResultDto;
import com.rvce.scas.dto.response.RoomAvailabilityDto;
import com.rvce.scas.dto.response.SeatingDashboardStateDto;
import com.rvce.scas.dto.response.SeatingSessionDto;
import com.rvce.scas.dto.response.StudentPublishedExamDto;
import com.rvce.scas.dto.response.StudentSeatAssignmentDto;
import com.rvce.scas.dto.response.TeacherAssignedExamDto;
import com.rvce.scas.security.JwtPrincipal;
import com.rvce.scas.service.ExamUploadService;
import com.rvce.scas.service.RoomAvailabilityService;
import com.rvce.scas.service.SeatingDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides exam-related endpoints used by controllers and reviewers.
 */
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamUploadService examUploadService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final SeatingDashboardService seatingDashboardService;

    /**
     * Get all exam sessions with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated list of exam sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Page<?>> listExamSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<?> sessions = examUploadService.getExamSessions(pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Create a new exam session.
     *
     * @param request   exam session payload
     * @param principal authenticated user
     * @return created exam session details
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<ExamSessionDto> createExamSession(
            @Valid @RequestBody CreateExamSessionRequest request,
            Authentication authentication) {
        ExamSessionDto created = examUploadService.createExamSession(request, resolveActorId(authentication));
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Get an exam session by id.
     *
     * @param examId the exam identifier
     * @return exam session details
     */
    @GetMapping("/{examId}")
    public ResponseEntity<ExamSessionDto> getExamSession(@PathVariable UUID examId) {
        ExamSessionDto session = examUploadService.getExamSession(Objects.requireNonNull(examId));
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{examId}/rooms/available")
    public ResponseEntity<List<RoomAvailabilityDto>> getAvailableRooms(@PathVariable UUID examId) {
        ExamSessionDto exam = examUploadService.getExamSession(Objects.requireNonNull(examId));
        LocalDate examDate = exam.getExamDate();
        LocalTime startTime = exam.getStartTime();
        LocalTime endTime = exam.getEndTime();
        List<RoomAvailabilityDto> rooms = roomAvailabilityService.getAvailable(examDate, startTime, endTime, null, null);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/{examId}/seating/session")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<SeatingSessionDto> openSeatingSession(
            @PathVariable UUID examId,
            Authentication authentication) {
        SeatingSessionDto session = seatingDashboardService.openSession(Objects.requireNonNull(examId), resolveActorId(authentication));
        return ResponseEntity.status(201).body(session);
    }

    @PostMapping("/students/upload")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<ExamStudentUploadResultDto> uploadStudents(
            @RequestParam UUID examId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        ExamStudentUploadResultDto result = examUploadService.uploadStudents(
                Objects.requireNonNull(examId),
                Objects.requireNonNull(file),
                resolveActorId(authentication));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{examId}/seating/state")
    public ResponseEntity<SeatingDashboardStateDto> loadSeatingDashboardState(@PathVariable UUID examId) {
        SeatingDashboardStateDto state = seatingDashboardService.loadState(examId);
        return ResponseEntity.ok(state);
    }

    @PatchMapping("/{examId}/seats/bulk-save")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<SeatingDashboardStateDto> bulkSaveSeats(
            @PathVariable UUID examId,
            @Valid @RequestBody BulkSeatSaveRequest request,
            Authentication authentication) {
        SeatingDashboardStateDto state = seatingDashboardService.bulkSave(Objects.requireNonNull(examId), request, resolveActorId(authentication));
        return ResponseEntity.ok(state);
    }

    @DeleteMapping("/{examId}/seats/clear-hall/{hallId}")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<Void> clearHall(
            @PathVariable UUID examId,
            @PathVariable UUID hallId,
            Authentication authentication) {
        seatingDashboardService.clearHall(Objects.requireNonNull(examId), Objects.requireNonNull(hallId), resolveActorId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish exam seating arrangement.
     *
     * @param examId the exam identifier to publish
     * @param principal authenticated user
     * @return published exam session details
     */
    @PostMapping("/{examId}/publish")
    @PreAuthorize("hasAnyRole('DEPT_COORD','ADMIN','TTO','EXAM_CONTROLLER')")
    public ResponseEntity<ExamSessionDto> publish(
            @PathVariable UUID examId,
            Authentication authentication) {
        ExamSessionDto published = seatingDashboardService.publishExam(Objects.requireNonNull(examId), resolveActorId(authentication));
        return ResponseEntity.ok(published);
    }

    @GetMapping("/student/seating")
    @PreAuthorize("hasRole('STUDENT') or hasAnyAuthority('EXAM_READ', 'NOTIFICATIONS_READ')")
    public ResponseEntity<List<StudentSeatAssignmentDto>> getStudentPublishedSeating(Authentication authentication) {
        UUID studentId = resolveActorId(authentication);
        return ResponseEntity.ok(seatingDashboardService.getPublishedSeatsForStudent(studentId));
    }

    @GetMapping("/student/exams")
    @PreAuthorize("hasRole('STUDENT') or hasAnyAuthority('EXAM_READ', 'NOTIFICATIONS_READ')")
    public ResponseEntity<List<StudentPublishedExamDto>> getStudentPublishedExams(Authentication authentication) {
        UUID studentId = resolveActorId(authentication);
        return ResponseEntity.ok(seatingDashboardService.getPublishedExamsForStudent(studentId));
    }

    @GetMapping("/{examId}/seating/my-seat")
    @PreAuthorize("hasRole('STUDENT') or hasAnyAuthority('EXAM_READ', 'NOTIFICATIONS_READ')")
    public ResponseEntity<StudentSeatAssignmentDto> getStudentSeatForExam(
            @PathVariable UUID examId,
            Authentication authentication) {
        UUID studentId = resolveActorId(authentication);
        StudentSeatAssignmentDto assignment = seatingDashboardService.getPublishedSeatForStudentByExam(
                Objects.requireNonNull(examId),
                studentId
        );
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/teacher/exams")
    @PreAuthorize("hasRole('TEACHER') or hasAnyAuthority('EXAM_READ', 'NOTIFICATIONS_READ')")
    public ResponseEntity<List<TeacherAssignedExamDto>> getTeacherAssignedExams(Authentication authentication) {
        UUID teacherId = resolveActorId(authentication);
        return ResponseEntity.ok(seatingDashboardService.getAssignedExamsForTeacher(teacherId));
    }

    @GetMapping("/{examId}/seating/view")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    public ResponseEntity<SeatingDashboardStateDto> viewSeating(@PathVariable UUID examId, Authentication authentication) {
        UUID userId = resolveActorId(authentication);
        SeatingDashboardStateDto state = seatingDashboardService.getSeatingStateForViewing(examId, userId);
        return ResponseEntity.ok(state);
    }

    private UUID resolveActorId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated principal is required.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.getUserId();
        }
        if (principal instanceof Principal simplePrincipal) {
            try {
                return UUID.fromString(simplePrincipal.getName());
            } catch (IllegalArgumentException ignored) {
                // Fall through to error below.
            }
        }
        throw new IllegalArgumentException("Unable to resolve authenticated user ID from principal type: " + principal.getClass().getName());
    }
}
