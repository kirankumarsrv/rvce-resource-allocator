package com.rvce.scas.service;

import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.entity.ExamHall;
import com.rvce.scas.entity.ExamSeat;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.entity.Room;
import com.rvce.scas.exception.ExamSessionNotFoundException;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSeatRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import com.rvce.scas.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for exam publish flow.
 * Verifies:
 * - State machine enforcement (DRAFT -> CONFIGURED -> PUBLISHED)
 * - Student assignment validation
 * - Hall capacity validation
 * - Event emission
 */
@SpringBootTest
@Transactional
@DisplayName("Exam Publish Flow Integration Tests")
class PublishFlowIntegrationTest {

    @Autowired
    private SeatingDashboardService seatingDashboardService;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private ExamStudentRepository examStudentRepository;

    @Autowired
    private ExamSeatRepository examSeatRepository;

    @Autowired
    private ExamHallRepository examHallRepository;

    @Autowired
    private RoomRepository roomRepository;

    private UUID examId;
    private UUID hallId;
    private UUID roomId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        setupTestData();
    }

    private void setupTestData() {
        // Create room
        Room room = new Room();
        room.setName("LAB-101");
        room.setDisplayName("Computer Lab 101");
        room.setRoomType("EXAM_HALL");
        room.setBenchRows(2);
        room.setBenchCols(2);
        room.setCapacity(4);
        room.setFloorNumber(1);
        room.setBlock("A");
        room = roomRepository.save(room);
        roomId = room.getId();

        // Create exam session
        ExamSession exam = new ExamSession();
        exam.setName("Data Structures Mid Sem");
        exam.setSubjectCode("CS201");
        exam.setSubjectName("Data Structures");
        exam.setSemester((short) 3);
        exam.setDepartmentId(UUID.randomUUID());
        exam.setExamDate(LocalDate.now().plusDays(7));
        exam.setStartTime(LocalTime.of(10, 0));
        exam.setEndTime(LocalTime.of(12, 0));
        exam.setStatus(ExamSession.ExamStatus.DRAFT);
        exam.setCreatedBy(actorId);
        exam = examSessionRepository.save(exam);
        examId = exam.getExamId();

        // Create exam hall
        ExamHall hall = new ExamHall();
        hall.setExamSession(exam);
        hall.setRoom(room);
        hall.setAssignedCapacity((short) 4);
        hall.setTotalBenches((short) 2);
        hall.setTwoSeaterCount((short) 2);
        hall.setThreeSeaterCount((short) 0);
        hall.setTotalCapacity((short) 4);
        hall.setBenchRows((short) 2);
        hall.setBenchCols((short) 2);
        hall.setSortOrder((short) 1);
        hall = examHallRepository.save(hall);
        hallId = hall.getHallId();

        // Create exam students
        for (int i = 0; i < 4; i++) {
            ExamStudent student = new ExamStudent();
            student.setExamId(examId);
            student.setUsn("1RV21CS" + String.format("%03d", i));
            student.setStudentName("Student " + i);
            student.setBranchCode("CS");
            student.setStudentId(UUID.randomUUID());
            examStudentRepository.save(student);
        }

        // Assign seats for all students
        var students = examStudentRepository.findByExamIdOrderByCreatedAtAsc(examId);
        for (int i = 0; i < students.size(); i++) {
            ExamSeat seat = new ExamSeat();
            seat.setExamSession(exam);
            seat.setHall(hall);
            seat.setStudentId(students.get(i).getStudentId());
            seat.setBenchRow((short) (i / 2 + 1));
            seat.setBenchCol((short) (i % 2 + 1));
            seat.setBenchSeatIndex((short) 0);
            seat.setBenchNumber("A-" + (i + 1));
            seat.setStatus("ASSIGNED");
            examSeatRepository.save(seat);
        }

        // Update exam to CONFIGURED
        exam.setStatus(ExamSession.ExamStatus.CONFIGURED);
        examSessionRepository.save(exam);
    }

    @Test
    @DisplayName("Should successfully publish exam when all students are assigned")
    void testPublishSuccessWithAllStudentsAssigned() {
        // Verify exam is in CONFIGURED state
        ExamSession exam = examSessionRepository.findById(examId).orElseThrow();
        assertEquals(ExamSession.ExamStatus.CONFIGURED, exam.getStatus());
        assertEquals(0, examStudentRepository.countUnassignedStudents(examId));

        // Publish
        ExamSessionDto result = seatingDashboardService.publishExam(examId, actorId);

        // Verify state changed
        assertEquals(ExamSession.ExamStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());

        // Verify in database
        ExamSession saved = examSessionRepository.findById(examId).orElseThrow();
        assertEquals(ExamSession.ExamStatus.PUBLISHED, saved.getStatus());
        assertNotNull(saved.getPublishedAt());
    }

    @Test
    @DisplayName("Should reject publish when students are unassigned")
    void testPublishFailsWithUnassignedStudents() {
        // Remove one seat assignment
        var seats = examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId);
        if (!seats.isEmpty()) {
            examSeatRepository.delete(seats.get(0));
        }

        // Attempt to publish should fail
        assertThrows(
            IllegalArgumentException.class,
            () -> seatingDashboardService.publishExam(examId, actorId),
            "Should reject publish when students are unassigned"
        );

        // Verify exam status didn't change
        ExamSession exam = examSessionRepository.findById(examId).orElseThrow();
        assertEquals(ExamSession.ExamStatus.CONFIGURED, exam.getStatus());
    }

    @Test
    @DisplayName("Should reject publish from PUBLISHED status (no re-publish)")
    void testPublishRejectedWhenAlreadyPublished() {
        // Publish first time
        seatingDashboardService.publishExam(examId, actorId);

        // Try to publish again
        assertThrows(
            IllegalArgumentException.class,
            () -> seatingDashboardService.publishExam(examId, actorId),
            "Should not allow re-publishing of already published exam"
        );
    }

    @Test
    @DisplayName("Should provide clear error message for unassigned students")
    void testPublishErrorMessageIncludesStudentCount() {
        // Remove all seat assignments
        var seats = examSeatRepository.findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(examId);
        seats.forEach(examSeatRepository::delete);

        // Attempt publish
        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> seatingDashboardService.publishExam(examId, actorId)
        );

        // Verify error message is informative
        String message = exception.getMessage();
        assertTrue(message.contains("4 of 4"), "Error should specify total students");
        assertTrue(message.contains("not assigned"), "Error should be clear about the issue");
    }

    @Test
    @DisplayName("Should verify exam exists before publish attempt")
    void testPublishRejectsNonExistentExam() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(
            ExamSessionNotFoundException.class,
            () -> seatingDashboardService.publishExam(nonExistentId, actorId)
        );
    }

    @Test
    @DisplayName("Should validate hall capacity not exceeded before publish")
    void testPublishRejectsWhenHallCapacityExceeded() {
        // Manually create over-capacity seating (hack for test)
        // This would normally be caught by bulkSave validation, but publish provides final check
        ExamSession exam = examSessionRepository.findById(examId).orElseThrow();
        ExamHall hall = examHallRepository.findByHallIdAndExamSession_ExamId(hallId, examId).orElseThrow();
        
        // Modify hall capacity to be less than assigned seats (simulating data anomaly)
        hall.setTotalCapacity((short) 2);
        examHallRepository.save(hall);

        // Publish should fail with capacity error
        assertThrows(
            IllegalArgumentException.class,
            () -> seatingDashboardService.publishExam(examId, actorId),
            "Should reject publish when seat count exceeds hall capacity"
        );
    }
}
