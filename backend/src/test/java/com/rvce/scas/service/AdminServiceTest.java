package com.rvce.scas.service;

import com.rvce.scas.dto.request.CreateUserRequest;
import com.rvce.scas.dto.request.ResetPasswordRequest;
import com.rvce.scas.dto.response.UserCreatedDto;
import com.rvce.scas.entity.Department;
import com.rvce.scas.entity.Role;
import com.rvce.scas.entity.User;
import com.rvce.scas.entity.UserRole;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.repository.RoleRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for admin user management.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                roleRepository,
                departmentRepository,
                userRoleRepository,
                passwordEncoder
        );
    }

    @Test
    void createUserAssignsTeacherRoleAndReturnsTempPassword() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Department department = new Department(departmentId, "Computer Science & Engineering", "CSE", null, null, null);
        Role role = new Role();
        role.setRoleId(roleId);
        role.setName("TEACHER");

        when(userRepository.findByEmailIgnoreCase("teacher@rvce.edu.in")).thenReturn(Optional.empty());
        when(departmentRepository.findByCodeIgnoreCase("CSE")).thenReturn(Optional.of(department));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0, String.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(userId);
            return user;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserRequest request = new CreateUserRequest(
                "Dr. Test Teacher",
                "teacher@rvce.edu.in",
                null,
                "TEACHER",
                "CSE"
        );

        UserCreatedDto result = adminService.createUser(request);

        assertEquals(userId, result.getUserId());
        assertEquals("teacher@rvce.edu.in", result.getEmail());
        assertEquals("Dr. Test Teacher", result.getName());
        assertEquals("TEACHER", result.getRole());
        assertEquals("Computer Science & Engineering", result.getDepartment());
        assertTrue(result.getTempPassword().startsWith("Temp_"));

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, times(1)).save(userRoleCaptor.capture());
        assertEquals(userId, userRoleCaptor.getValue().getId().getUserId());
        assertEquals(roleId, userRoleCaptor.getValue().getId().getRoleId());
    }

    @Test
    void createUserAssignsAdminRoleToo() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Department department = new Department(departmentId, "Administration", "ADMIN", null, null, null);
        Role role = new Role();
        role.setRoleId(roleId);
        role.setName("ADMIN");

        when(userRepository.findByEmailIgnoreCase("admin.dev@rvce.edu.in")).thenReturn(Optional.empty());
        when(departmentRepository.findByCodeIgnoreCase("ADMIN")).thenReturn(Optional.of(department));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0, String.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(userId);
            return user;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserCreatedDto result = adminService.createUser(new CreateUserRequest(
                "Admin Dev",
                "admin.dev@rvce.edu.in",
                null,
                "ADMIN",
                "ADMIN"
        ));

        assertEquals("ADMIN", result.getRole());
        assertTrue(result.getTempPassword().startsWith("Temp_"));
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
    }

    @Test
    void createUserRejectsInvalidEmailDomain() {
        CreateUserRequest request = new CreateUserRequest(
                "Invalid Domain",
                "invalid.user@example.com",
                null,
                "TEACHER",
                "CSE"
        );

        try {
            adminService.createUser(request);
            assertTrue(false, "Expected IllegalArgumentException for invalid email domain");
        } catch (IllegalArgumentException ex) {
            assertEquals("Email must be a valid @rvce.edu.in address", ex.getMessage());
        }
    }

    @Test
    void createUserRejectsMalformedEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "Bad Email",
                "not-an-email",
                null,
                "TEACHER",
                "CSE"
        );

        try {
            adminService.createUser(request);
            assertTrue(false, "Expected IllegalArgumentException for malformed email");
        } catch (IllegalArgumentException ex) {
            assertEquals("Email must be a valid @rvce.edu.in address", ex.getMessage());
        }
    }

    @Test
    void resetPasswordReturnsTemporaryPasswordPayload() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        user.setEmail("teacher@rvce.edu.in");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0, String.class));

        Map<String, String> result = adminService.resetPassword(userId, new ResetPasswordRequest("Forgot password"));

        assertEquals("teacher@rvce.edu.in", result.get("email"));
        assertTrue(result.get("tempPassword").startsWith("Temp_"));
        assertTrue(result.get("message").contains("temporary password"));
        verify(userRepository).save(user);
    }
}