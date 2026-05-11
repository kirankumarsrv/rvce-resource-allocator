package com.rvce.scas.config;

import com.rvce.scas.entity.Department;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * DevDataInitializer
 *
 * Initializes test user passwords in development environments.
 * Only active when spring.profiles.active=dev
 *
 * This replaces the hardcoded password hash migration (V10) with
 * a runtime initialization that doesn't expose secrets in git.
 */
@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    public CommandLineRunner initDevUsers(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            JdbcTemplate jdbcTemplate,
            BCryptPasswordEncoder encoder) {
        return args -> {
            String[] devEmails = {
                "admin@rvce.edu.in",
                "tto@rvce.edu.in",
                "priya.sharma@rvce.edu.in",
                "ramesh.kumar@rvce.edu.in",
                "kiran@rvce.edu.in",
                "anil.kumar@rvce.edu.in",
                "lakshmi.narayana@rvce.edu.in",
                "farah.khan@rvce.edu.in"
            };

            String testPassword = "Test@1234";
            String encodedPassword = encoder.encode(testPassword);

            for (String email : devEmails) {
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    user.setPasswordHash(encodedPassword);
                    userRepository.save(user);
                    System.out.println("✓ Dev mode: Reset password for " + email);
                });
            }

            Department cse = departmentRepository.findByNameIgnoreCase("Computer Science & Engineering")
                    .orElseThrow();
            Department ise = departmentRepository.findByNameIgnoreCase("Information Science & Engineering")
                    .orElseThrow();
            Department ece = departmentRepository.findByNameIgnoreCase("Electronics & Communication Engineering")
                    .orElseThrow();

            upsertTeacher(userRepository, departmentRepository, jdbcTemplate, encodedPassword,
                    "44444444-4444-4444-4444-444444444018",
                    "Dr. Anil Kumar",
                    "anil.kumar@rvce.edu.in",
                    cse.getDepartmentId(),
                    "22222222-2222-2222-2222-222222222001",
                    "44444444-4444-4444-4444-444444444001");
            upsertTeacher(userRepository, departmentRepository, jdbcTemplate, encodedPassword,
                    "44444444-4444-4444-4444-444444444019",
                    "Dr. Lakshmi Narayana",
                    "lakshmi.narayana@rvce.edu.in",
                    ise.getDepartmentId(),
                    "22222222-2222-2222-2222-222222222001",
                    "44444444-4444-4444-4444-444444444001");
            upsertTeacher(userRepository, departmentRepository, jdbcTemplate, encodedPassword,
                    "44444444-4444-4444-4444-444444444020",
                    "Dr. Farah Khan",
                    "farah.khan@rvce.edu.in",
                    ece.getDepartmentId(),
                    "22222222-2222-2222-2222-222222222001",
                    "44444444-4444-4444-4444-444444444001");

            System.out.println("✓ Dev data initialization complete. Test password: Test@1234");
        };
    }

    private void upsertTeacher(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            JdbcTemplate jdbcTemplate,
            String encodedPassword,
            String userId,
            String name,
            String email,
            UUID departmentId,
            String teacherRoleId,
            String grantedBy) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        UUID seededUserId = UUID.fromString(userId);

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Instant now = Instant.now();
            Timestamp nowTs = Timestamp.from(now);
            jdbcTemplate.update(
                    """
                    INSERT INTO users (
                        user_id, name, email, password_hash,
                        is_active, failed_login_count, version,
                        department_id, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id) DO UPDATE SET
                        name = EXCLUDED.name,
                        email = EXCLUDED.email,
                        password_hash = EXCLUDED.password_hash,
                        is_active = EXCLUDED.is_active,
                        failed_login_count = EXCLUDED.failed_login_count,
                        updated_at = EXCLUDED.updated_at
                    """,
                    seededUserId,
                    name,
                    email,
                    encodedPassword,
                    true,
                    0,
                    0,
                    department.getDepartmentId(),
                    nowTs,
                    nowTs);

            return userRepository.findById(seededUserId)
                    .orElseThrow(() -> new IllegalStateException("Failed to seed teacher user: " + seededUserId));
        });

        if (user.getDepartment() == null) {
            user.setDepartment(department);
            userRepository.save(user);
        }

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id, granted_by) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
            user.getUserId(),
                java.util.UUID.fromString(teacherRoleId),
                java.util.UUID.fromString(grantedBy));
    }
}
