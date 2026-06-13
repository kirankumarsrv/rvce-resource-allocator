package com.rvce.scas.service;

import com.rvce.scas.dto.request.ExamHallConfigRequest;
import com.rvce.scas.dto.response.ExamHallDto;
import com.rvce.scas.dto.response.HallGridDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.Role;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.User;
import com.rvce.scas.dto.response.ExamSeatDto;
import com.rvce.scas.exception.ExamHallConflictException;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.RoleRepository;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for hall configuration and bench grid rendering.
 */
@ExtendWith(MockitoExtension.class)
class ExamHallServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamHallRepository examHallRepository;

    @Mock
    private ExamSeatRepository examSeatRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private ExamMapper examMapper;

    private ExamHallService examHallService;

    @BeforeEach
    void setUp() {
        examHallService = new ExamHallService(
                examSessionRepository,
                examHallRepository,
                examSeatRepository,
                roomRepository,
                userRepository,
                roleRepository,
                userRoleRepository,
                examMapper,
                new AuditService(),
                new BenchLayoutBuilder()
        );
    }

    @Test
    void addHallStoresBenchMixAndCapacity() {
        UUID examId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        UUID invigilatorId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");
        room.setRoomType("EXAM_HALL");
        room.setCapacity(120);
        room.setBenchRows(10);
        room.setBenchCols(12);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        User invigilator = new User();
        invigilator.setUserId(invigilatorId);
        when(userRepository.findById(invigilatorId)).thenReturn(Optional.of(invigilator));

        Role teacherRole = new Role();
        teacherRole.setRoleId(UUID.randomUUID());
        teacherRole.setName("ROLE_TEACHER");
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRoleRepository.existsByUser_UserIdAndRole_RoleId(invigilatorId, teacherRole.getRoleId())).thenReturn(true);

        when(examHallRepository.existsByExamSession_ExamIdAndRoom_Id(examId, roomId)).thenReturn(false);
        when(examHallRepository.countByExamSession_ExamId(examId)).thenReturn(0L);
        when(examHallRepository.save(any(ExamHall.class))).thenAnswer(invocation -> {
            ExamHall hall = invocation.getArgument(0);
            hall.setHallId(hallId);
            return hall;
        });
        when(examMapper.toDto(any(ExamHall.class))).thenAnswer(invocation -> {
            ExamHall hall = invocation.getArgument(0);
            ExamHallDto dto = new ExamHallDto();
            dto.setHallId(hall.getHallId());
            dto.setExamId(examId);
            dto.setRoomId(roomId);
            dto.setRoomName(room.getName());
            dto.setRoomDisplayName(room.getDisplayName());
            dto.setTwoSeaterCount(hall.getTwoSeaterCount() != null ? hall.getTwoSeaterCount().intValue() : null);
            dto.setThreeSeaterCount(hall.getThreeSeaterCount() != null ? hall.getThreeSeaterCount().intValue() : null);
            dto.setTotalCapacity(hall.getTotalCapacity() != null ? hall.getTotalCapacity().intValue() : null);
            dto.setBenchRows(hall.getBenchRows() != null ? hall.getBenchRows().intValue() : null);
            dto.setBenchCols(hall.getBenchCols() != null ? hall.getBenchCols().intValue() : null);
            dto.setInvigilatorId(invigilatorId);
            dto.setSortOrder(hall.getSortOrder() != null ? hall.getSortOrder().intValue() : null);
            return dto;
        });

        ExamHallConfigRequest request = new ExamHallConfigRequest();
        request.setRoomId(roomId);
        request.setTwoSeaterCount(4);
        request.setThreeSeaterCount(2);
        request.setInvigilatorId(invigilatorId.toString());

        ExamHallDto response = examHallService.addHall(examId, request, UUID.randomUUID());

        assertEquals(hallId, response.getHallId());
        assertEquals(4, response.getTwoSeaterCount());
        assertEquals(2, response.getThreeSeaterCount());
        assertEquals(14, response.getTotalCapacity());
        assertEquals(10, response.getBenchRows());
        assertEquals(12, response.getBenchCols());
        assertEquals("D101", response.getRoomName());
    }

    @Test
    void getHallGridUsesBenchCountsAndSeatOccupancy() {
        UUID examId = UUID.randomUUID();
        UUID hallId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.CONFIGURED);

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");

        ExamHall hall = new ExamHall();
        hall.setHallId(hallId);
        hall.setExamSession(examSession);
        hall.setRoom(room);
        hall.setTwoSeaterCount((short) 2);
        hall.setThreeSeaterCount((short) 1);
        hall.setTotalCapacity((short) 7);
        hall.setBenchRows((short) 2);
        hall.setBenchCols((short) 3);

        when(examHallRepository.findByHallIdAndExamSession_ExamId(hallId, examId)).thenReturn(Optional.of(hall));

        ExamSeat occupiedSeat = new ExamSeat();
        occupiedSeat.setSeatId(UUID.randomUUID());
        occupiedSeat.setExamSession(examSession);
        occupiedSeat.setHall(hall);
        occupiedSeat.setStudentId(UUID.randomUUID());
        occupiedSeat.setBenchRow((short) 1);
        occupiedSeat.setBenchCol((short) 1);
        occupiedSeat.setBenchSeatIndex((short) 0);
        occupiedSeat.setBenchNumber("A-1");
        occupiedSeat.setManualOverride(false);

        when(examSeatRepository.findByExamSession_ExamIdAndHall_HallId(examId, hallId))
                .thenReturn(List.of(occupiedSeat));

        when(examMapper.toDto(any(ExamSeat.class))).thenAnswer(invocation -> {
            ExamSeat seat = invocation.getArgument(0);
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
            return dto;
        });

        HallGridDto response = examHallService.getHallGrid(examId, hallId);

        assertEquals(2, response.getBenchRows());
        assertEquals(3, response.getBenchCols());
        assertEquals(1, response.getGrid().get(0).get(0).getOccupiedCount());
        assertEquals(2, response.getGrid().get(0).get(0).getSeatCapacity());
        assertTrue(response.getGrid().get(0).get(0).isActive());
        assertTrue(response.getGrid().get(1).get(0).isExcluded());
        assertFalse(response.getGrid().get(1).get(0).isActive());
    }

    @Test
    void addHallRejectsDuplicateRoomConfiguration() {
        UUID examId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");
        room.setRoomType("EXAM_HALL");
        room.setCapacity(120);
        room.setBenchRows(10);
        room.setBenchCols(12);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        when(examHallRepository.existsByExamSession_ExamIdAndRoom_Id(examId, roomId)).thenReturn(true);

        ExamHallConfigRequest request = new ExamHallConfigRequest();
        request.setRoomId(roomId);
        request.setTwoSeaterCount(4);
        request.setThreeSeaterCount(2);

        assertThrows(ExamHallConflictException.class, () -> examHallService.addHall(examId, request, UUID.randomUUID()));
    }

    @Test
    void addHallRejectsNonTeacherInvigilator() {
        UUID examId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID invigilatorId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");
        room.setRoomType("EXAM_HALL");
        room.setCapacity(120);
        room.setBenchRows(10);
        room.setBenchCols(12);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        User invigilator = new User();
        invigilator.setUserId(invigilatorId);
        when(userRepository.findById(invigilatorId)).thenReturn(Optional.of(invigilator));

        Role teacherRole = new Role();
        teacherRole.setRoleId(UUID.randomUUID());
        teacherRole.setName("ROLE_TEACHER");
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRoleRepository.existsByUser_UserIdAndRole_RoleId(invigilatorId, teacherRole.getRoleId())).thenReturn(false);

        when(examHallRepository.existsByExamSession_ExamIdAndRoom_Id(examId, roomId)).thenReturn(false);

        ExamHallConfigRequest request = new ExamHallConfigRequest();
        request.setRoomId(roomId);
        request.setTwoSeaterCount(4);
        request.setThreeSeaterCount(2);
        request.setInvigilatorId(invigilatorId.toString());

        assertThrows(IllegalArgumentException.class, () -> examHallService.addHall(examId, request, UUID.randomUUID()));
    }

    @Test
    void addHallRejectsInvalidInvigilatorIdFormat() {
        UUID examId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));

        Room room = new Room();
        room.setId(roomId);
        room.setName("D101");
        room.setDisplayName("Block D - Examination Block");
        room.setRoomType("EXAM_HALL");
        room.setCapacity(120);
        room.setBenchRows(10);
        room.setBenchCols(12);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        when(examHallRepository.existsByExamSession_ExamIdAndRoom_Id(examId, roomId)).thenReturn(false);

        ExamHallConfigRequest request = new ExamHallConfigRequest();
        request.setRoomId(roomId);
        request.setTwoSeaterCount(4);
        request.setThreeSeaterCount(2);
        request.setInvigilatorId("not-a-uuid");

        assertThrows(IllegalArgumentException.class, () -> examHallService.addHall(examId, request, UUID.randomUUID()));
    }
}
