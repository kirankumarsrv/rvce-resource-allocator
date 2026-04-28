package com.rvce.scas.rbac;

import com.rvce.scas.service.AuthService;
import com.rvce.scas.security.JwtTokenProvider;
import com.rvce.scas.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("T-006: RBAC Authorization Tests")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

        @MockBean
        private UserDetailsServiceImpl userDetailsService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("TTO can upload timetable -> 200")
    @WithMockUser(username = "tto@rvce.edu.in", authorities = {"ROLE_TTO", "TIMETABLE_WRITE", "TIMETABLE_READ"})
    void tto_canUploadTimetable() throws Exception {
        mockMvc.perform(post("/api/timetable/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("academicYear", "2025-26")
                        .param("semester", "5"))
                .andExpect(status().isOk());
    }

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

    @Test
    @DisplayName("EXAM_CONTROLLER can publish seating plan -> 200")
    @WithMockUser(authorities = {"ROLE_EXAM_CONTROLLER", "EXAM_PUBLISH", "EXAM_WRITE"})
    void examController_canPublish() throws Exception {
        mockMvc.perform(post("/api/exam/99999999-9999-9999-9999-999999999001/publish"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TTO cannot access audit logs -> 403")
    @WithMockUser(authorities = {"ROLE_TTO", "TIMETABLE_WRITE", "TIMETABLE_GENERATE"})
    void tto_cannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint -> 401")
    void unauthenticated_gets401() throws Exception {
        mockMvc.perform(get("/api/rooms/availability"))
                .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

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
