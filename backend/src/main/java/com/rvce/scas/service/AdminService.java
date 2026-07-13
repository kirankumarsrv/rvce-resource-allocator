package com.rvce.scas.service;

import com.rvce.scas.dto.request.CreateUserRequest;
import com.rvce.scas.dto.request.ResetPasswordRequest;
import com.rvce.scas.dto.response.TeacherListDto;
import com.rvce.scas.dto.response.UserCreatedDto;
import com.rvce.scas.entity.Department;
import com.rvce.scas.entity.Role;
import com.rvce.scas.entity.UserRole;
import com.rvce.scas.entity.UserRoleId;
import com.rvce.scas.entity.User;
import com.rvce.scas.exception.CsvValidationException;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.repository.RoleRepository;
import com.rvce.scas.repository.UserRoleRepository;
import com.rvce.scas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for admin operations: user creation, password reset, teacher listing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user with the specified details.
     * Generates a temporary password that the user must change on first login.
     *
     * @param request user creation request (name, email, role, department)
     * @return UserCreatedDto with new user ID and temp password
     * @throws CsvValidationException if email already exists or role/department invalid
     */
    private static final Pattern RVCE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@rvce\\.edu\\.in$", Pattern.CASE_INSENSITIVE);

    @Transactional
    public UserCreatedDto createUser(CreateUserRequest request) {
        validateCreateUserRequest(request);

        // Validate email is unique
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new CsvValidationException("User with email " + request.getEmail() + " already exists");
        }

        // Find department
        Department department = departmentRepository.findByCodeIgnoreCase(request.getDepartmentCode())
                .orElseThrow(() -> new CsvValidationException("Department not found: " + request.getDepartmentCode()));

        // Find role
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new CsvValidationException("Role not found: " + request.getRole()));

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(tempPassword);

        // Create user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(hashedPassword);
        user.setUsn(request.getUsn());
        user.setDepartment(department);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        assignRoleToUser(savedUser, role);

        log.info("Created new user: {} ({}) with role: {}", user.getEmail(), user.getName(), request.getRole());

        // Return created user DTO with temp password
        return new UserCreatedDto(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getName(),
                tempPassword,
                request.getRole(),
                department.getName()
        );
    }

    /**
     * Bulk create users with the specified details.
     * Generates temporary passwords for each user.
     *
     * @param requests list of user creation requests
     * @return List of UserCreatedDto with new user IDs and temp passwords
     */
    @Transactional
    public List<UserCreatedDto> bulkCreateUsers(List<CreateUserRequest> requests) {
        return requests.stream()
                .map(this::createUser)
                .toList();
    }

    private void assignRoleToUser(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getUserId(), role.getRoleId()));
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("User creation request cannot be null");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!RVCE_EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Email must be a valid @rvce.edu.in address");
        }
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getDepartmentCode() == null || request.getDepartmentCode().isBlank()) {
            throw new IllegalArgumentException("Department code is required");
        }
        if ("STUDENT".equalsIgnoreCase(request.getRole()) && (request.getUsn() == null || request.getUsn().isBlank())) {
            throw new IllegalArgumentException("USN is required for student users");
        }
    }

    /**
     * Reset a user's password to a temporary value.
     *
     * @param userId target user ID
     * @param request reset request (reason)
     * @return Map with new temp password and confirmation message
     * @throws CsvValidationException if user not found
     */
    @Transactional
    public Map<String, String> resetPassword(UUID userId, ResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CsvValidationException("User not found: " + userId));

        String tempPassword = generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(tempPassword);

        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        log.info("Reset password for user: {} (reason: {})", user.getEmail(), request.getReason());

        return Map.of(
                "email", user.getEmail(),
                "tempPassword", tempPassword,
                "message", "Password reset successfully. User should log in with the temporary password and change it immediately."
        );
    }

    /**
     * List all teachers for invigilator assignment dropdown.
     *
     * @return List of TeacherListDto
     */
    @Transactional(readOnly = true)
    public List<TeacherListDto> listTeachers() {
        return userRepository.findAllByRoleName("TEACHER")
                .stream()
                .map(user -> new TeacherListDto(
                        user.getUserId(),
                        user.getName(),
                        user.getEmail(),
                        user.getDepartment() != null ? user.getDepartment().getCode() : "UNKNOWN"
                ))
                .collect(Collectors.toList());
    }

    /**
     * Generate a random temporary password.
     * Format: Temp_XXXXXX (where X = random alphanumeric)
     *
     * @return temporary password
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return "Temp_" + password.toString();
    }
}
