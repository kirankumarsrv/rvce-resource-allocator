package com.rvce.scas.service;

import com.rvce.scas.dto.request.BulkSeatSaveRequest;
import com.rvce.scas.dto.request.SeatPlacementRequest;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.SeatingDashboardStateDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.entity.Room;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the manual seating dashboard flow.
 */
@ExtendWith(MockitoExtension.class)
class SeatingDashboardServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamHallRepository examHallRepository;

    @Mock
    private ExamSeatRepository examSeatRepository;

    @Mock
    private ExamStudentRepository examStudentRepository;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private SeatingDashboardService seatingDashboardService;

    @BeforeEach
    void setUp() {
        seatingDashboardService = new SeatingDashboardService(
                examSessionRepository,
                examHallRepository,
                examSeatRepository,
                examStudentRepository,
                examMapper,
                new BenchLayoutBuilder(),
                new AuditService(),
                eventPublisher
        );
    }

    @Test
    void loadStateIncludesAssignedAndUnassignedStudents() {
        UUID examId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.CONFIGURED);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");

        ExamHall hall = new ExamHall();
        hall.setHallId(hallId);
        hall.setExamSession(examSession);
        hall.setRoom(room);
        hall.setTwoSeaterCount((short) 1);
        hall.setThreeSeaterCount((short) 0);
        hall.setBenchRows((short) 1);
        hall.setBenchCols((short) 1);
        hall.setSortOrder((short) 1);

        when(examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId)).thenReturn(List.of(hall));

        ExamHallDto hallDto = new ExamHallDto();
        hallDto.setHallId(hallId);
        hallDto.setExamId(examId);
        hallDto.setRoomId(roomId);
        hallDto.setRoomName("D101");
        hallDto.setRoomDisplayName("Block D - Examination Block");
        hallDto.setTwoSeaterCount(1);
        hallDto.setThreeSeaterCount(0);
        hallDto.setTotalCapacity(2);
        hallDto.setBenchRows(1);
        hallDto.setBenchCols(1);
        when(examMapper.toHallDtoList(List.of(hall))).thenReturn(List.of(hallDto));

        ExamStudent seatedStudent = new ExamStudent();
        seatedStudent.setEntryId(UUID.randomUUID());
        seatedStudent.setExamId(examId);
        seatedStudent.setStudentId(studentUserId);
        seatedStudent.setUsn("1RV23CS001");
        seatedStudent.setStudentName("Asha");
        seatedStudent.setBranchCode("CSE");

        ExamStudent unassignedStudent = new ExamStudent();
        unassignedStudent.setEntryId(UUID.randomUUID());
        unassignedStudent.setExamId(examId);
        unassignedStudent.setUsn("1RV23CS002");
        unassignedStudent.setStudentName("Bharath");
        unassignedStudent.setBranchCode("CSE");

        when(examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId)).thenReturn(List.of(seatedStudent, unassignedStudent));

        ExamSeat seat = new ExamSeat();
        seat.setSeatId(UUID.randomUUID());
        seat.setExamSession(examSession);
        seat.setHall(hall);
        seat.setStudentId(studentUserId);
        seat.setBenchRow((short) 1);
        seat.setBenchCol((short) 1);
        seat.setBenchSeatIndex((short) 0);
        seat.setBenchNumber("A-1");
        seat.setManualOverride(true);
        when(examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId))
                .thenReturn(List.of(seat));
        when(examSeatRepository.findByExamSession_ExamIdAndHall_HallId(examId, hallId)).thenReturn(List.of(seat));

        SeatingDashboardStateDto response = seatingDashboardService.loadState(examId);

        assertEquals(1, response.getAssignedCount());
        assertEquals(2, response.getTotalCount());
        assertEquals(1, response.getUnassignedStudents().size());
        assertEquals("1RV23CS002", response.getUnassignedStudents().get(0).getUsn());
        assertEquals(1, response.getHallGrids().get(0).getGrid().get(0).get(0).getOccupiedCount());
        assertEquals(2, response.getHallGrids().get(0).getGrid().get(0).get(0).getSeatCapacity());
    }

    @Test
    void bulkSaveReplacesAssignmentsAndReturnsUpdatedState() {
        UUID examId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.CONFIGURED);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");

        ExamHall hall = new ExamHall();
        hall.setHallId(hallId);
        hall.setExamSession(examSession);
        hall.setRoom(room);
        hall.setTwoSeaterCount((short) 1);
        hall.setThreeSeaterCount((short) 0);
        hall.setBenchRows((short) 1);
        hall.setBenchCols((short) 1);
        hall.setSortOrder((short) 1);

        when(examHallRepository.findByExamSession_ExamIdOrderBySortOrderAsc(examId)).thenReturn(List.of(hall));

        ExamHallDto hallDto = new ExamHallDto();
        hallDto.setHallId(hallId);
        hallDto.setExamId(examId);
        hallDto.setRoomId(roomId);
        hallDto.setRoomName("D101");
        hallDto.setRoomDisplayName("Block D - Examination Block");
        hallDto.setTwoSeaterCount(1);
        hallDto.setThreeSeaterCount(0);
        hallDto.setTotalCapacity(2);
        hallDto.setBenchRows(1);
        hallDto.setBenchCols(1);
        when(examMapper.toHallDtoList(List.of(hall))).thenReturn(List.of(hallDto));

        ExamStudent student = new ExamStudent();
        student.setEntryId(UUID.randomUUID());
        student.setExamId(examId);
        student.setStudentId(studentUserId);
        student.setUsn("1RV23CS001");
        student.setStudentName("Asha");
        student.setBranchCode("CSE");
        when(examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId)).thenReturn(List.of(student));

        ExamSeat seat = new ExamSeat();
        seat.setSeatId(UUID.randomUUID());
        seat.setExamSession(examSession);
        seat.setHall(hall);
        seat.setStudentId(studentUserId);
        seat.setBenchRow((short) 1);
        seat.setBenchCol((short) 1);
        seat.setBenchSeatIndex((short) 0);
        seat.setBenchNumber("A-1");
        seat.setManualOverride(true);
        when(examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId))
                .thenReturn(List.of(seat));
        when(examSeatRepository.findByExamSession_ExamIdAndHall_HallId(examId, hallId)).thenReturn(List.of(seat));
        doNothing().when(examSeatRepository).deleteByExamId(examId);
        when(examSeatRepository.saveAll(any())).thenReturn(List.of(seat));

        BulkSeatSaveRequest request = new BulkSeatSaveRequest();
        SeatPlacementRequest assignment = new SeatPlacementRequest();
        assignment.setStudentId(studentUserId);
        assignment.setHallId(hallId);
        assignment.setBenchRow((short) 1);
        assignment.setBenchCol((short) 1);
        assignment.setBenchSeatIndex((short) 0);
        request.setAssignments(List.of(assignment));

        SeatingDashboardStateDto response = seatingDashboardService.bulkSave(examId, request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(1, response.getAssignedCount());
        assertEquals(1, response.getHallGrids().size());
        assertEquals(1, response.getAssignedSeats().size());
        assertEquals("1RV23CS001", response.getAssignedSeats().get(0).getUsn());
    }
}
