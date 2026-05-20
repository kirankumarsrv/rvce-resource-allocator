package com.rvce.scas.service;

import com.rvce.scas.dto.request.ExamHallConfigRequest;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.Role;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.ExamHallConflictException;
import com.rvce.scas.exception.ExamHallNotFoundException;
import com.rvce.scas.exception.ExamSessionNotFoundException;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.RoleRepository;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for configuring exam halls and producing hall grids.
 */
@Service
@RequiredArgsConstructor
public class ExamHallService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamHallRepository examHallRepository;
    private final ExamSeatRepository examSeatRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ExamMapper examMapper;
    private final AuditService auditService;
    private final BenchLayoutBuilder benchLayoutBuilder;

    @Transactional
    public ExamHallDto addHall(@NonNull UUID examId, @NonNull ExamHallConfigRequest request, @NonNull UUID actorId) {
        ExamSession examSession = requireExamSession(examId);
        ensureMutable(examSession);

        Room room = roomRepository.findById(Objects.requireNonNull(request.getRoomId()))
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.getRoomId()));

        if (room.getRoomType() == null || !"EXAM_HALL".equalsIgnoreCase(room.getRoomType())) {
            throw new IllegalArgumentException("Room " + room.getName() + " is not configured as an exam hall.");
        }
        if (room.getBenchRows() == null || room.getBenchCols() == null) {
            throw new IllegalArgumentException("Exam hall room " + room.getName() + " is missing bench grid dimensions.");
        }

        int activeBenches = request.getTwoSeaterCount() + request.getThreeSeaterCount();
        int physicalBenchSlots = room.getBenchRows() * room.getBenchCols();
        int totalCapacity = (request.getTwoSeaterCount() * 2) + (request.getThreeSeaterCount() * 3);

        if (activeBenches > physicalBenchSlots) {
            throw new IllegalArgumentException("Configured bench count cannot exceed the physical bench grid size.");
        }
        if (totalCapacity > room.getCapacity()) {
            throw new IllegalArgumentException("Configured seat capacity cannot exceed the room capacity.");
        }
        if (examHallRepository.existsByExamSession_ExamIdAndRoom_Id(examId, room.getId())) {
            throw new ExamHallConflictException("Room already configured for this exam.");
        }

        // Validate and resolve invigilator (mandatory)
        User invigilator = validateAndResolveInvigilator(request.getInvigilatorId());

        if (examHallRepository.existsByExamSession_ExamIdAndInvigilator_UserId(examId, invigilator.getUserId())) {
            throw new ExamHallConflictException("Invigilator " + invigilator.getName() + " is already assigned to another hall for this exam.");
        }

        ExamHall hall = new ExamHall();
        hall.setExamSession(examSession);
        hall.setRoom(room);
        hall.setAssignedCapacity((short) totalCapacity);
        hall.setTotalBenches((short) activeBenches);
        hall.setTwoSeaterCount(request.getTwoSeaterCount().shortValue());
        hall.setThreeSeaterCount(request.getThreeSeaterCount().shortValue());
        hall.setTotalCapacity((short) totalCapacity);
        hall.setBenchRows(room.getBenchRows().shortValue());
        hall.setBenchCols(room.getBenchCols().shortValue());
        hall.setSortOrder((short) ((int) examHallRepository.countByExamSession_ExamId(examId) + 1));
        hall.setInvigilator(invigilator);

        ExamHall saved = examHallRepository.save(hall);
        auditService.log(actorId, "ADD_EXAM_HALL", "exam_halls", saved.getHallId());
        return examMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExamHallDto> getHalls(@NonNull UUID examId) {
        requireExamSession(examId);
        return examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId).stream()
                .map(examMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HallGridDto getHallGrid(@NonNull UUID examId, @NonNull UUID hallId) {
        ExamHall hall = requireHall(examId, hallId);
        List<ExamSeat> seats = examSeatRepository.findByExamSession_ExamIdAndHall_HallId(examId, hallId);
        List<ExamSeatDto> seatDtos = seats.stream().map(examMapper::toDto).toList();
        return benchLayoutBuilder.buildHallGrid(hall, seatDtos);
    }

    @Transactional
    public void deleteHall(@NonNull UUID examId, @NonNull UUID hallId, @NonNull UUID actorId) {
        ExamHall hall = requireHall(examId, hallId);
        ensureMutable(hall.getExamSession());
        examSeatRepository.deleteByExamIdAndHallId(examId, hallId);
        examHallRepository.delete(hall);
        auditService.log(actorId, "DELETE_EXAM_HALL", "exam_halls", hallId);
    }

    private ExamSession requireExamSession(@NonNull UUID examId) {
        return examSessionRepository.findById(examId)
                .orElseThrow(() -> new ExamSessionNotFoundException("Exam session not found: " + examId));
    }

    private ExamHall requireHall(@NonNull UUID examId, @NonNull UUID hallId) {
        return examHallRepository.findByHallIdAndExamSession_ExamId(hallId, examId)
                .orElseThrow(() -> new ExamHallNotFoundException("Exam hall not found: " + hallId));
    }

    private void ensureMutable(ExamSession examSession) {
        if (examSession.getStatus() != ExamSession.ExamStatus.DRAFT
                && examSession.getStatus() != ExamSession.ExamStatus.CONFIGURED) {
            throw new IllegalArgumentException("Exam halls can only be modified while the exam is in DRAFT or CONFIGURED status.");
        }
    }

    /**
     * Validates and resolves an invigilator by ID.
     * Ensures the invigilator exists and has ROLE_TEACHER.
     *
     * @param invigilatorId the invigilator ID (must be a valid UUID string)
     * @return the resolved User with teacher role
     * @throws IllegalArgumentException if the ID is invalid, user not found, or user is not a teacher
     */
    private User validateAndResolveInvigilator(@NonNull String invigilatorId) {
        UUID invigilatorUUID;
        try {
            invigilatorUUID = UUID.fromString(invigilatorId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid invigilator ID format: " + invigilatorId, e);
        }

        User invigilator = userRepository.findById(invigilatorUUID)
                .orElseThrow(() -> new IllegalArgumentException("Invigilator not found: " + invigilatorId));

        // Verify invigilator has TEACHER role (matches seed data role name)
        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseThrow(() -> new IllegalArgumentException("TEACHER role not found in system"));

        boolean isTeacher = userRoleRepository.existsByUser_UserIdAndRole_RoleId(invigilatorUUID, teacherRole.getRoleId());
        if (!isTeacher) {
            throw new IllegalArgumentException("User " + invigilatorId + " is not assigned the TEACHER role");
        }

        return invigilator;
    }
}