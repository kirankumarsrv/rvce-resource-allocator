package com.rvce.scas.service.integration;

import com.rvce.scas.entity.ExamSession;
import com.rvce.scas.entity.Role;
import com.rvce.scas.entity.Room;
import com.rvce.scas.entity.User;
import com.rvce.scas.entity.UserRole;
import com.rvce.scas.entity.UserRoleId;
import com.rvce.scas.repository.ExamHallRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import com.rvce.scas.repository.RoleRepository;
import com.rvce.scas.repository.RoomRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.repository.UserRoleRepository;
import com.rvce.scas.service.email.EmailService;
import com.rvce.scas.security.JwtPrincipal;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for exam hall creation API behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T-104 Integration: Exam Hall Creation")
class ExamHallCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private ExamHallRepository examHallRepository;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        examHallRepository.deleteAll();
        examSessionRepository.deleteAll();
        roomRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Valid teacher invigilator accepted for exam hall creation -> 201")
    void validTeacherInvigilatorCreatesExamHall() throws Exception {
        UUID actorId = UUID.randomUUID();
        Role teacherRole = createRole("TEACHER");
        User teacher = createUser("teacher@rvce.edu.in");
        assignRole(teacher, teacherRole);

        ExamSession examSession = createExamSession(actorId);
        Room examHallRoom = createExamHallRoom();

        String requestJson = String.format(
                "{\"roomId\":\"%s\",\"twoSeaterCount\":10,\"threeSeaterCount\":5,\"invigilatorId\":\"%s\"}",
                examHallRoom.getId(), teacher.getUserId());

        JwtPrincipal principal = new JwtPrincipal(
                actorId,
                "exam-controller@rvce.edu.in",
                List.of(
                        new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"),
                        new SimpleGrantedAuthority("EXAM_WRITE")
                )
        );

        mockMvc.perform(post("/api/exam/%s/halls".formatted(examSession.getExamId()))
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invigilatorId").value(teacher.getUserId().toString()))
                .andExpect(jsonPath("$.roomId").value(examHallRoom.getId().toString()));

        assertThat(examHallRepository.countByExamSession_ExamId(examSession.getExamId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Missing invigilatorId triggers request validation -> 400")
    void missingInvigilatorIdReturnsBadRequest() throws Exception {
        UUID actorId = UUID.randomUUID();
        createRole("TEACHER");
        ExamSession examSession = createExamSession(actorId);
        Room examHallRoom = createExamHallRoom();

        String requestJson = String.format(
                "{\"roomId\":\"%s\",\"twoSeaterCount\":10,\"threeSeaterCount\":5}",
                examHallRoom.getId());

        JwtPrincipal principal = new JwtPrincipal(
                actorId,
                "exam-controller@rvce.edu.in",
                List.of(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
        );

        mockMvc.perform(post("/api/exam/%s/halls".formatted(examSession.getExamId()))
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.invigilatorId").value("Invigilator is required"));
    }

    @Test
    @DisplayName("Non-teacher invigilator is rejected by business validation -> 400")
    void nonTeacherInvigilatorRejected() throws Exception {
        UUID actorId = UUID.randomUUID();
        Role teacherRole = createRole("TEACHER");
        User staff = createUser("staff@rvce.edu.in");
        // Do not assign TEACHER to this user.
        assignRole(staff, createRole("ROLE_STAFF"));

        ExamSession examSession = createExamSession(actorId);
        Room examHallRoom = createExamHallRoom();

        String requestJson = String.format(
                "{\"roomId\":\"%s\",\"twoSeaterCount\":10,\"threeSeaterCount\":5,\"invigilatorId\":\"%s\"}",
                examHallRoom.getId(), staff.getUserId());

        JwtPrincipal principal = new JwtPrincipal(
                actorId,
                "exam-controller@rvce.edu.in",
                List.of(
                        new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"),
                        new SimpleGrantedAuthority("EXAM_WRITE")
                )
        );

        mockMvc.perform(post("/api/exam/%s/halls".formatted(examSession.getExamId()))
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TEACHER")));
    }

    private ExamSession createExamSession(UUID creatorId) {
        ExamSession session = new ExamSession();
        session.setName("Midterm Exam");
        session.setSubjectCode("MTH101");
        session.setSubjectName("Calculus I");
        session.setSection("A");
        session.setSemester((short) 1);
        session.setDepartmentId(UUID.randomUUID());
        session.setExamDate(LocalDate.now().plusDays(7));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(12, 0));
        session.setStatus(ExamSession.ExamStatus.DRAFT);
        session.setCreatedBy(creatorId);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        return examSessionRepository.save(session);
    }

    private Room createExamHallRoom() {
        Room room = new Room();
        room.setName("EXAM-HALL-1");
        room.setDisplayName("Exam Hall 1");
        room.setRoomType("EXAM_HALL");
        room.setCapacity(100);
        room.setBenchRows(10);
        room.setBenchCols(10);
        room.setFloorNumber(1);
        room.setBlock("A");
        room.setBuilding("Main Block");
        room.setLatitude(null);
        room.setLongitude(null);
        room.setDirectionsText("First floor, east wing");
        room.setDeptOwnerId(null);
        room.setIsActive(true);
        room.setCreatedAt(Instant.now());
        room.setUpdatedAt(Instant.now());
        return roomRepository.save(room);
    }

    private User createUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setUsn(null);
        user.setPasswordHash("password");
        user.setActive(true);
        user.setFailedLoginCount((short) 0);
        user.setLastLoginAt(null);
        user.setLockedUntil(null);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        return roleRepository.save(role);
    }

    private void assignRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getUserId(), role.getRoleId()));
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }
}
