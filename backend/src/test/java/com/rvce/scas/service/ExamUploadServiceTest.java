package com.rvce.scas.service;

import com.rvce.scas.dto.request.CreateExamSessionRequest;
import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.ExamStudent;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.ExamStudentCsvValidationException;
import com.rvce.scas.mapper.ExamMapper;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.ExamStudentRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.validation.UsnValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for T-401 exam upload service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-401: Exam Upload Service Tests")
class ExamUploadServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamStudentRepository examStudentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.rvce.scas.repository.DepartmentRepository departmentRepository;
    @Mock
    private ExamMapper examMapper;

    private ExamUploadService examUploadService;

    private final UsnValidator usnValidator = new UsnValidator();

    @BeforeEach
    void setUp() {
        examUploadService = new ExamUploadService(
            examSessionRepository,
            examStudentRepository,
            userRepository,
            departmentRepository,
            examMapper,
            usnValidator,
            new AuditService()
        );
    }

    @Test
    @DisplayName("createExamSession should persist a DRAFT session")
    void createExamSessionPersistsDraftSession() {
        UUID actorId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();

        CreateExamSessionRequest request = new CreateExamSessionRequest();
        request.setName("Dec 2025 CIE-3 - 5th Sem CSE");
        request.setSubjectCode("21CS51");
        request.setSubjectName("Design and Analysis of Algorithms");
        request.setSemester(5);
        request.setDepartmentId(UUID.randomUUID());
        request.setExamDate(LocalDate.of(2025, 12, 10));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));

        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(invocation -> {
            ExamSession session = invocation.getArgument(0);
            session.setExamId(examId);
            return session;
        });
        when(examMapper.toDto(any(ExamSession.class))).thenAnswer(invocation -> {
            ExamSession session = invocation.getArgument(0);
            ExamSessionDto dto = new ExamSessionDto();
            dto.setExamId(session.getExamId());
            dto.setName(session.getName());
            dto.setSubjectCode(session.getSubjectCode());
            dto.setSubjectName(session.getSubjectName());
            dto.setSemester(session.getSemester() == null ? null : session.getSemester().intValue());
            dto.setDepartmentId(session.getDepartmentId());
            dto.setExamDate(session.getExamDate());
            dto.setStartTime(session.getStartTime());
            dto.setEndTime(session.getEndTime());
            dto.setStatus(session.getStatus());
            dto.setCreatedBy(session.getCreatedBy());
            dto.setStudentCount(0L);
            return dto;
        });

        ExamSessionDto response = examUploadService.createExamSession(request, actorId);

        assertNotNull(response);
        assertEquals(examId, response.getExamId());
        assertEquals(ExamSession.ExamStatus.DRAFT, response.getStatus());
        assertEquals(0L, response.getStudentCount());
        verify(examSessionRepository).save(any(ExamSession.class));
    }

    @Test
    @DisplayName("uploadStudents should persist valid CSV rows")
    void uploadStudentsPersistsValidRows() {
        UUID examId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));
        when(examStudentRepository.findUsnsByExamId(examId)).thenReturn(List.of());

        User user = new User();
        user.setUserId(studentId);
        user.setUsn("1RV22CS050");
        when(userRepository.findAllByRoleName("STUDENT")).thenReturn(List.of(user));
        when(examStudentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "usn,name,branch_code,needs_front_row\n"
                + "1RV22CS050,Kiran Kumar,CSE,false\n"
                + "1RV22CS051,Priya Mehta,ISE,1\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        var result = examUploadService.uploadStudents(examId, file, actorId);

        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getInserted());
        assertEquals(0, result.getSkipped());
        assertEquals(0, result.getErrors().size());
        verify(examStudentRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("uploadStudents should reject duplicate USNs in the same CSV")
    void uploadStudentsRejectsDuplicateUsns() {
        UUID examId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        ExamSession examSession = new ExamSession();
        examSession.setExamId(examId);
        examSession.setStatus(ExamSession.ExamStatus.DRAFT);
        when(examSessionRepository.findById(examId)).thenReturn(Optional.of(examSession));
        when(examStudentRepository.findUsnsByExamId(examId)).thenReturn(List.of());

        String csv = "usn,name,branch_code,needs_front_row\n"
                + "1RV22CS050,Kiran Kumar,CSE,false\n"
                + "1RV22CS050,Priya Mehta,ISE,false\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ExamStudentCsvValidationException exception = assertThrows(
                ExamStudentCsvValidationException.class,
                () -> examUploadService.uploadStudents(examId, file, actorId));

        assertEquals(2, exception.getResult().getTotalRows());
        assertEquals(0, exception.getResult().getInserted());
        assertEquals(2, exception.getResult().getSkipped());
        assertEquals(1, exception.getResult().getErrors().size());
        assertEquals(3, exception.getResult().getErrors().get(0).getRow());
    }
}
