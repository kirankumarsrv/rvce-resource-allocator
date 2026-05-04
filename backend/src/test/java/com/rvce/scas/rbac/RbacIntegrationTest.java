package com.rvce.scas.rbac;

import com.rvce.scas.dto.response.UploadResultDto;
import com.rvce.scas.security.JwtTokenProvider;
import com.rvce.scas.security.UserDetailsServiceImpl;
import com.rvce.scas.service.AuthService;
import com.rvce.scas.service.TimetableUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the application's role-based access control boundaries.
 */
@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T-006: RBAC Authorization Tests")
class RbacIntegrationTest {

    // REVIEW-GAP: this suite covers authorization boundaries, but it does not yet verify
    // the account lockout flow. T-005's important security rule is: 5 failed logins should
    // produce a lockout response (ideally 429 TOO_MANY_REQUESTS if that is the chosen API contract).
    // Add a dedicated login-path integration test for that behavior when you are ready.

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TimetableUploadService uploadService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Confirms that a TTO user can upload timetable data.
     *
     * @throws Exception if MockMvc cannot perform the request
     */
    @Test
    @DisplayName("TTO can upload timetable -> 200")
    @WithMockUser(username = "tto@rvce.edu.in", authorities = {"ROLE_TTO", "TIMETABLE_WRITE", "TIMETABLE_READ"})
    void tto_canUploadTimetable() throws Exception {
        when(uploadService.upload(any())).thenReturn(new UploadResultDto());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "timetable.csv",
                "text/csv",
                "room_id,teacher_id,day_of_week,start_time,end_time,subject,department\n".getBytes());

        mockMvc.perform(multipart("/api/timetable/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

            /**
             * Confirms that a student is denied timetable upload access.
             *
             * @throws Exception if MockMvc cannot perform the request
             */
            @Test
    @DisplayName("STUDENT cannot upload timetable -> 403 JSON")
    @WithMockUser(username = "student@rvce.edu.in", authorities = {"ROLE_STUDENT", "TIMETABLE_READ", "EXAM_READ"})
    void student_cannotUploadTimetable() throws Exception {
        mockMvc.perform(post("/api/timetable/upload").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PERMISSIONS"))
                .andExpect(jsonPath("$.path").value("/api/timetable/upload"));
    }

    /**
     * Confirms that an exam controller user can publish an exam endpoint.
     *
     * @throws Exception if MockMvc cannot perform the request
     */
    @Test
    @DisplayName("EXAM_CONTROLLER can publish seating plan -> 200")
    @WithMockUser(authorities = {"ROLE_EXAM_CONTROLLER", "EXAM_PUBLISH", "EXAM_WRITE"})
    void examController_canPublish() throws Exception {
        mockMvc.perform(post("/api/exam/99999999-9999-9999-9999-999999999001/publish"))
                .andExpect(status().isOk());
    }

    /**
     * Confirms that TTO users cannot access admin audit logs.
     *
     * @throws Exception if MockMvc cannot perform the request
     */
    @Test
    @DisplayName("TTO cannot access audit logs -> 403")
    @WithMockUser(authorities = {"ROLE_TTO", "TIMETABLE_WRITE", "TIMETABLE_GENERATE"})
    void tto_cannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    /**
     * Confirms that unauthenticated callers receive a JSON 401 response.
     *
     * @throws Exception if MockMvc cannot perform the request
     */
    @Test
    @DisplayName("Unauthenticated request to protected endpoint -> 401")
    void unauthenticated_gets401() throws Exception {
        mockMvc.perform(get("/api/rooms/availability"))
                .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * Confirms that the login endpoint remains publicly reachable.
     *
     * @throws Exception if MockMvc cannot perform the request
     */
    @Test
    @DisplayName("Unauthenticated request to /api/auth/login -> 401 from auth service")
    void login_endpoint_is_public() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
