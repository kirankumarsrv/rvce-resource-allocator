package com.rvce.scas.controller;

import com.rvce.scas.dto.request.CreateUserRequest;
import com.rvce.scas.dto.request.ResetPasswordRequest;
import com.rvce.scas.dto.response.TeacherListDto;
import com.rvce.scas.dto.response.UserCreatedDto;
import com.rvce.scas.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles admin-facing endpoints for operational and audit visibility.
 * Also handles user management (create users, reset passwords).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Returns a lightweight success payload for the audit log endpoint.
     *
     * @return HTTP 200 response with a simple status marker
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, String>> auditLogs() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Create a new user (teacher or student).
     * Only ADMIN role can create users.
     *
     * @param request user creation request (name, email, role, department)
     * @return HTTP 201 with created user details + temp password
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserCreatedDto> createUser(@RequestBody CreateUserRequest request) {
        UserCreatedDto result = adminService.createUser(request);
        return ResponseEntity.status(201).body(result);
    }

    /**
     * Bulk create users (teachers, students, admins, etc.).
     * Only ADMIN role can perform bulk operations.
     *
     * @param requests list of user creation requests
     * @return HTTP 201 with list of created user details + temp passwords
     */
    @PostMapping("/users/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserCreatedDto>> bulkCreateUsers(@RequestBody List<CreateUserRequest> requests) {
        List<UserCreatedDto> results = requests.stream()
                .map(adminService::createUser)
                .toList();
        return ResponseEntity.status(201).body(results);
    }

    /**
     * Reset a user's password to a temporary value.
     * Only ADMIN role can reset passwords.
     *
     * @param userId target user ID
     * @param request password reset request
     * @return HTTP 200 with reset details
     */
    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable UUID userId,
            @RequestBody ResetPasswordRequest request) {
        Map<String, String> result = adminService.resetPassword(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * List all teachers (for invigilator assignment dropdown).
     * Available to DEPT_COORD and ADMIN roles.
     *
     * @return list of teachers with name, email, department
     */
    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEPT_COORD')")
    public ResponseEntity<List<TeacherListDto>> listTeachers() {
        List<TeacherListDto> teachers = adminService.listTeachers();
        return ResponseEntity.ok(teachers);
    }
}
