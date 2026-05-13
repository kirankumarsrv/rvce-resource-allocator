package com.rvce.scas.controller;

import com.rvce.scas.dto.request.CreateUserRequest;
import com.rvce.scas.dto.response.UserCreatedDto;
import com.rvce.scas.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Development-only admin endpoints.
 * Active only when Spring profile 'dev' is enabled.
 *
 * Provides convenience APIs to create teacher/admin accounts and return
 * temporary passwords for use in development environments.
 */
@RestController
@RequestMapping("/api/admin/dev")
@RequiredArgsConstructor
@Profile("dev")
public class AdminDevController {

    private final AdminService adminService;

    /**
     * Create a single user (DEV only). No security applied by design — runs only in 'dev' profile.
     */
    @PostMapping("/users")
    public ResponseEntity<UserCreatedDto> createUserDev(@RequestBody CreateUserRequest request) {
        UserCreatedDto created = adminService.createUser(request);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Bulk create users in development. Returns list of created users with temporary passwords.
     */
    @PostMapping("/users/bulk")
    public ResponseEntity<List<UserCreatedDto>> bulkCreateUsersDev(@RequestBody List<CreateUserRequest> requests) {
        List<UserCreatedDto> created = requests.stream()
                .map(adminService::createUser)
                .collect(Collectors.toList());
        return ResponseEntity.status(201).body(created);
    }
}
