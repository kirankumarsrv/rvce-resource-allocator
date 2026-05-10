package com.rvce.scas.service;

import com.rvce.scas.dto.request.BulkSeatSaveRequest;
import com.rvce.scas.dto.request.SeatPlacementRequest;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.dto.response.SeatingDashboardStateDto;
import com.rvce.scas.dto.response.SeatingSessionDto;
import com.rvce.scas.dto.response.UnassignedStudentDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.exception.ExamHallNotFoundException;
import com.rvce.scas.exception.ExamSessionNotFoundException;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import jakarta.persistence.OptimisticLockException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manual seating dashboard service.
 */
@Service
@RequiredArgsConstructor
public class SeatingDashboardService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamHallRepository examHallRepository;
    private final ExamSeatRepository examSeatRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExamMapper examMapper;
    private final BenchLayoutBuilder benchLayoutBuilder;
    private final AuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public SeatingSessionDto openSession(UUID examId, UUID actorId) {
        ensureExamExists(examId);

        SeatingSessionDto response = new SeatingSessionDto();
        response.setExamId(examId);
        response.setSessionId(examId);
        response.setOpenedAt(Instant.now());
        return response;
    }

    @Transactional(readOnly = true)
    public SeatingDashboardStateDto loadState(UUID examId) {
        ExamSession examSession = ensureExamExists(examId);
        List<ExamHall> halls = examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId);
        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId);

        Map<UUID, ExamStudent> studentsByUserId = students.stream()
                .filter(student -> student.getStudentId() != null)
                .collect(Collectors.toMap(ExamStudent::getStudentId, student -> student, (left, right) -> left));

        Map<UUID, ExamSeat> seatByStudentId = seats.stream()
                .collect(Collectors.toMap(seat -> Objects.requireNonNull(seat.getStudentId()), seat -> seat, (left, right) -> left));

        List<ExamHallDto> hallDtos = examMapper.toHallDtoList(halls);

        List<HallGridDto> hallGrids = halls.stream()
                .map(hall -> benchLayoutBuilder.buildHallGrid(hall, examSeatRepository.findByExamSession_ExamIdAndHall_HallId(examId, hall.getHallId())))
                .toList();

        List<ExamSeatDto> seatDtos = seats.stream()
                .map(seat -> enrichSeatDto(seat, studentsByUserId.get(seat.getStudentId())))
                .sorted(Comparator.comparing(ExamSeatDto::getBenchRow)
                        .thenComparing(ExamSeatDto::getBenchCol)
                        .thenComparing(ExamSeatDto::getBenchSeatIndex))
                .toList();

        List<UnassignedStudentDto> unassignedStudents = students.stream()
                .filter(student -> student.getStudentId() == null || !seatByStudentId.containsKey(student.getStudentId()))
                .map(student -> toUnassignedStudent(student))
                .toList();

        SeatingDashboardStateDto response = new SeatingDashboardStateDto();
        response.setExamId(examSession.getExamId());
        response.setSessionId(examSession.getExamId());
        response.setHalls(hallDtos);
        response.setHallGrids(hallGrids);
        response.setAssignedSeats(seatDtos);
        response.setUnassignedStudents(unassignedStudents);
        response.setAssignedCount(seatDtos.size());
        response.setTotalCount(students.size());
        return response;
    }

    @Transactional
    public SeatingDashboardStateDto bulkSave(@NonNull UUID examId, BulkSeatSaveRequest request, @NonNull UUID actorId) {
        ExamSession examSession = ensureExamExists(examId);
        ensureMutable(examSession);

        List<ExamHall> halls = examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId);
        Map<UUID, ExamHall> hallById = halls.stream().collect(Collectors.toMap(ExamHall::getHallId, hall -> hall));
        List<ExamStudent> students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        Map<UUID, ExamStudent> studentByUserId = students.stream()
                .filter(student -> student.getStudentId() != null)
                .collect(Collectors.toMap(student -> Objects.requireNonNull(student.getStudentId()), student -> student, (left, right) -> left));

        if (request == null || request.getAssignments() == null) {
            throw new IllegalArgumentException("Seat assignments are required.");
        }

        Map<UUID, Set<String>> positionsByHall = new HashMap<>();
        Set<UUID> seenStudentIds = new HashSet<>();
        List<ExamSeat> seats = new ArrayList<>();

        for (SeatPlacementRequest assignment : request.getAssignments()) {
            if (assignment == null) {
                throw new IllegalArgumentException("Seat assignment entries cannot be null.");
            }

            ExamHall hall = hallById.get(assignment.getHallId());
            if (hall == null) {
                throw new ExamHallNotFoundException("Exam hall not found: " + assignment.getHallId());
            }

            ExamStudent student = studentByUserId.get(assignment.getStudentId());
            if (student == null) {
                throw new IllegalArgumentException("Student is not enrolled or does not have a linked user account: " + assignment.getStudentId());
            }

            validatePlacement(hall, assignment, student, positionsByHall, seenStudentIds);

            ExamSeat seat = new ExamSeat();
            seat.setExamSession(examSession);
            seat.setHall(hall);
            seat.setStudentId(assignment.getStudentId());
            seat.setBenchRow(assignment.getBenchRow());
            seat.setBenchCol(assignment.getBenchCol());
            seat.setBenchSeatIndex(assignment.getBenchSeatIndex());
            seat.setBenchNumber(benchLayoutBuilder.benchLabel(assignment.getBenchRow(), assignment.getBenchCol()));
            seat.setManualOverride(true);
            seat.setStatus("ASSIGNED");
            seats.add(seat);
        }

        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                examSeatRepository.deleteByExamId(examId);
                examSeatRepository.saveAll(seats);
                break; // Success, exit loop
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed to save seats after " + maxRetries + " attempts due to concurrent modifications", e);
                }
                // Reload data and retry
                halls = examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId);
                hallById = halls.stream().collect(Collectors.toMap(ExamHall::getHallId, hall -> hall));
                students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
                studentByUserId = students.stream()
                        .filter(student -> student.getStudentId() != null)
                        .collect(Collectors.toMap(
                                student -> Objects.requireNonNull(student.getStudentId()),
                                student -> student,
                                (left, right) -> left
                        ));
            }
        }
        auditService.log(actorId, "SAVE_SEATING_ASSIGNMENTS", "exam_seats", examId);
        return loadState(examId);
    }

    @Transactional
    public void clearHall(@NonNull UUID examId, @NonNull UUID hallId, @NonNull UUID actorId) {
        ensureMutable(ensureExamExists(examId));
        if (examHallRepository.findByHallIdAndExamSession_ExamId(hallId, examId).isEmpty()) {
            throw new ExamHallNotFoundException("Exam hall not found: " + hallId);
        }
        examSeatRepository.deleteByExamIdAndHallId(examId, hallId);
        auditService.log(actorId, "CLEAR_SEATING_HALL", "exam_seats", hallId);
    }

    private void validatePlacement(
            ExamHall hall,
            SeatPlacementRequest assignment,
            ExamStudent student,
            Map<UUID, Set<String>> positionsByHall,
            Set<UUID> seenStudentIds) {

        if (!seenStudentIds.add(assignment.getStudentId())) {
            throw new IllegalArgumentException("Student appears more than once in the bulk save payload: " + assignment.getStudentId());
        }

        int benchIndex = benchIndex(hall, assignment.getBenchRow(), assignment.getBenchCol());
        int capacity = benchCapacity(hall, benchIndex);
        if (capacity == 0) {
            throw new IllegalArgumentException("Selected bench is not active in hall " + hall.getHallId());
        }
        if (assignment.getBenchSeatIndex() >= capacity) {
            throw new IllegalArgumentException("Seat index exceeds the capacity of the selected bench.");
        }

        String positionKey = assignment.getBenchRow() + ":" + assignment.getBenchCol() + ":" + assignment.getBenchSeatIndex();
        positionsByHall.computeIfAbsent(hall.getHallId(), ignored -> new HashSet<>());
        if (!positionsByHall.get(hall.getHallId()).add(positionKey)) {
            throw new IllegalArgumentException("Duplicate seat position in bulk save payload: " + positionKey);
        }
    }

    private int benchCapacity(ExamHall hall, int benchIndex) {
        if (benchIndex < hall.getTwoSeaterCount()) {
            return 2;
        }
        if (benchIndex < hall.getTwoSeaterCount() + hall.getThreeSeaterCount()) {
            return 3;
        }
        return 0;
    }

    private int benchIndex(ExamHall hall, int row, int col) {
        return (row - 1) * hall.getBenchCols() + (col - 1);
    }

    private ExamSeatDto enrichSeatDto(ExamSeat seat, ExamStudent student) {
        ExamSeatDto dto = new ExamSeatDto();
        dto.setSeatId(seat.getSeatId());
        dto.setExamId(seat.getExamSession().getExamId());
        dto.setHallId(seat.getHall().getHallId());
        dto.setStudentId(seat.getStudentId());
        dto.setBenchRow(seat.getBenchRow());
        dto.setBenchCol(seat.getBenchCol());
        dto.setBenchSeatIndex(seat.getBenchSeatIndex());
        dto.setBenchNumber(seat.getBenchNumber());
        dto.setManualOverride(seat.isManualOverride());
        if (student != null) {
            dto.setUsn(student.getUsn());
            dto.setStudentName(student.getStudentName());
            dto.setBranchCode(student.getBranchCode());
        }
        return dto;
    }

    private UnassignedStudentDto toUnassignedStudent(ExamStudent student) {
        UnassignedStudentDto dto = new UnassignedStudentDto();
        dto.setEntryId(student.getEntryId());
        dto.setStudentId(student.getStudentId());
        dto.setUsn(student.getUsn());
        dto.setStudentName(student.getStudentName());
        dto.setBranchCode(student.getBranchCode());
        dto.setReason(student.getStudentId() == null ? "NO_USER_ACCOUNT" : "UNASSIGNED");
        return dto;
    }

    private ExamSession ensureExamExists(UUID examId) {
        return examSessionRepository.findById(examId)
                .orElseThrow(() -> new ExamSessionNotFoundException("Exam session not found: " + examId));
    }

    private void ensureMutable(ExamSession examSession) {
        if (examSession.getStatus() != ExamSession.ExamStatus.DRAFT
                && examSession.getStatus() != ExamSession.ExamStatus.CONFIGURED) {
            throw new IllegalArgumentException("Seating can only be modified while the exam is in DRAFT or CONFIGURED status.");
        }
    }

    /**
     * Publish exam seating arrangement
     * Validates all students are assigned, updates status to PUBLISHED, and emits event
     */
    @Transactional
    public com.rvce.scas.dto.response.ExamSessionDto publishExam(@NonNull UUID examId, @NonNull UUID actorId) {
        ExamSession examSession = ensureExamExists(examId);

        // Check all students are assigned
        long unassignedCount = examStudentRepository.countUnassignedStudents(examId);
        if (unassignedCount > 0) {
            throw new IllegalArgumentException(unassignedCount + " students are not assigned to seats");
        }

        // Update status
        examSession.setStatus(ExamSession.ExamStatus.PUBLISHED);
        examSession.setPublishedAt(Instant.now());
        ExamSession saved = examSessionRepository.save(examSession);

        // Audit
        auditService.log(actorId, "EXAM_PUBLISHED", "EXAM", examId);

        // Emit event for notifications
        eventPublisher.publishEvent(
            new com.rvce.scas.event.ExamPublishedEvent(
                this, examId, examSession.getName(), actorId
            )
        );

        return examMapper.toDto(saved);
    }
}