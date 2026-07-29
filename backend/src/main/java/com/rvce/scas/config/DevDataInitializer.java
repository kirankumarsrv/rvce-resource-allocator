package com.rvce.scas.config;

import com.rvce.scas.entity.Department;
import com.rvce.scas.entity.User;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.repository.UserRepository;
import com.rvce.scas.security.EmailHashUtil;
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

            // Clean up any legacy or invalid USN values before JPA loads them.
            jdbcTemplate.update(
                    "UPDATE users SET usn = NULL " +
                    "WHERE usn IS NOT NULL " +
                    "  AND usn !~ '^[0-9A-Z]{10,13}$' " +
                    "  AND usn !~ '^[A-Za-z0-9+/=]{24,100}$'"
            );

            for (String email : devEmails) {
                userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    user.setPasswordHash(encodedPassword);
                    // Fix any remaining invalid USN values that may have loaded from legacy data.
                    String usn = user.getUsn();
                    if (usn != null && !usn.matches("^[0-9A-Z]{10,13}$")) {
                        user.setUsn(null);
                    }
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
                    "44444444-4444-4444-4444-444444444021",
                    "Dr. Meera Rao",
                    "meera.rao@rvce.edu.in",
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

            seedTimetableSlots(jdbcTemplate);

            System.out.println("✓ Dev data initialization complete. Test password: Test@1234");
        };
    }

    private void seedTimetableSlots(JdbcTemplate jdbcTemplate) {
        String versionId = "66666666-6666-6666-6666-666666666001";
        String roomA201 = "55555555-5555-5555-5555-555500000003";
        String roomA202 = "55555555-5555-5555-5555-555500000004";
        String roomB101 = "55555555-5555-5555-5555-555500000007";
        String teacherPriya = "44444444-4444-4444-4444-444444444003";
        String teacherRamesh = "44444444-4444-4444-4444-444444444004";
        String teacherAnil = "44444444-4444-4444-4444-444444444018";
        String teacherMeera = "44444444-4444-4444-4444-444444444021";

        String[] statements = new String[]{
                "INSERT INTO timetable_versions (version_id, academic_year, semester, label, status, created_by) VALUES (?, '2025-26', 5, 'Odd Semester 2025-26 - Dev Seed', 'ACTIVE', '44444444-4444-4444-4444-444444444002') ON CONFLICT (version_id) DO NOTHING",
                "INSERT INTO timetable_slots (version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time, is_active) VALUES (?, ?, ?, 'Computer Science & Engineering', '21CS51', 'Design & Analysis of Algorithms', 'A', 5, 1, 1, '08:00', '09:00', true) ON CONFLICT DO NOTHING",
                "INSERT INTO timetable_slots (version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time, is_active) VALUES (?, ?, ?, 'Computer Science & Engineering', '21CS52', 'Operating Systems', 'B', 5, 1, 2, '09:00', '10:00', true) ON CONFLICT DO NOTHING",
                "INSERT INTO timetable_slots (version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time, is_active) VALUES (?, ?, ?, 'Computer Science & Engineering', '21CSL57', 'CN & OS Lab', 'A', 5, 3, 3, '10:15', '12:15', true) ON CONFLICT DO NOTHING",
                "INSERT INTO timetable_slots (version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time, is_active) VALUES (?, ?, ?, 'Computer Science & Engineering', '21CS53', 'Database Management Systems', 'C', 5, 2, 4, '11:00', '12:00', true) ON CONFLICT DO NOTHING",
                "INSERT INTO timetable_slots (version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time, is_active) VALUES (?, ?, ?, 'Computer Science & Engineering', '21CS54', 'Machine Learning', 'A', 5, 4, 5, '13:00', '14:00', true) ON CONFLICT DO NOTHING"
        };

        Object[][] params = new Object[][]{
                {versionId},
                {versionId, roomA201, teacherRamesh},
                {versionId, roomA202, teacherRamesh},
                {versionId, roomB101, teacherPriya},
                {versionId, roomA201, teacherAnil},
                {versionId, roomA202, teacherMeera}
        };

        for (int i = 0; i < statements.length; i++) {
            jdbcTemplate.update(statements[i], params[i]);
        }

        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('timetable_slots', 'slot_id'), COALESCE((SELECT MAX(slot_id) FROM timetable_slots), 1), true)");
        System.out.println("✓ Dev seed: timetable slots inserted for teacher schedule visibility");
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

        String emailHash = EmailHashUtil.hashEmail(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Instant now = Instant.now();
            Timestamp nowTs = Timestamp.from(now);
            jdbcTemplate.update(
                    """
                    INSERT INTO users (
                        user_id, name, email, email_hash, password_hash,
                        is_active, failed_login_count, version,
                        department_id, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id) DO UPDATE SET
                        name = EXCLUDED.name,
                        email = EXCLUDED.email,
                        email_hash = EXCLUDED.email_hash,
                        password_hash = EXCLUDED.password_hash,
                        is_active = EXCLUDED.is_active,
                        failed_login_count = EXCLUDED.failed_login_count,
                        updated_at = EXCLUDED.updated_at
                    """,
                    seededUserId,
                    name,
                    email,
                    emailHash,
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

    @Bean
    public CommandLineRunner seedSubstitutionConflict(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Seed an additional timetable slot for a replacement teacher to create a predictable clash
                // This uses existing seeded version '66666666-6666-6666-6666-666666666001' and room A201
                String sql = "INSERT INTO timetable_slots (slot_id, version_id, room_id, teacher_id, department, subject_code, subject_name, section, semester, day_of_week, period_number, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

                jdbcTemplate.update(sql,
                        9999, // slot_id unique in dev seed context
                        java.util.UUID.fromString("66666666-6666-6666-6666-666666666001"),
                        java.util.UUID.fromString("55555555-5555-5555-5555-555500000003"), // A201
                        java.util.UUID.fromString("44444444-4444-4444-4444-444444444018"), // replacement teacher seeded above
                        "Computer Science & Engineering",
                        "ZZTST",
                        "Substitution Conflict Test",
                        "A",
                        5,
                        1,
                        1,
                        "08:00",
                        "09:00"
                );

                jdbcTemplate.execute(
                        "SELECT setval(pg_get_serial_sequence('timetable_slots', 'slot_id'), COALESCE((SELECT MAX(slot_id) FROM timetable_slots), 1), true)"
                );

                System.out.println("✓ Dev seed: substitution conflict slot inserted (id=9999) and timetable slot sequence synced");
            } catch (Exception e) {
                System.out.println("Dev seed substitution conflict skipped: " + e.getMessage());
            }
        };
    }
}
