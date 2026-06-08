-- ============================================================
-- V23__seed_admin_exam_controller_roles.sql
-- SCAS - Add default admin and core role seed data for deployment
-- ============================================================

-- Expand the DB-level role constraint so the new role is accepted.
ALTER TABLE roles
    DROP CONSTRAINT IF EXISTS chk_role_name;

ALTER TABLE roles
    ADD CONSTRAINT chk_role_name CHECK (
        name IN ('STUDENT','TEACHER','TTO','DEPT_COORD','ADMIN','EXAM_CONTROLLER','SUPER_ADMIN','ANOTHER')
    );

-- Ensure the core deployment roles exist.
INSERT INTO roles (role_id, name, description) VALUES
    ('22222222-2222-2222-2222-222222222005', 'ADMIN',          'Full system access: users, rooms, config, analytics'),
    ('22222222-2222-2222-2222-222222222006', 'EXAM_CONTROLLER', 'Exam hall management and invigilator assignment'),
    ('22222222-2222-2222-2222-222222222001', 'STUDENT',        'Read-only access to exam seating, timetable and notifications'),
    ('22222222-2222-2222-2222-222222222002', 'TEACHER',        'Classroom and exam hall occupancy verification plus student view access'),
    ('22222222-2222-2222-2222-222222222008', 'ANOTHER',        'Additional custom role for deployment test accounts')
ON CONFLICT (name) DO NOTHING;

-- Seed a small set of deployment users with a known default password.
-- Password: BCrypt('Test@1234', 12)
INSERT INTO users (user_id, name, email, password_hash, usn, department_id) VALUES
    ('44444444-4444-4444-4444-444444444901', 'Seed Admin',         'admin@rvce.edu.in',          '$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO', NULL, '11111111-1111-1111-1111-111111111099'),
    ('44444444-4444-4444-4444-444444444902', 'Seed Exam Controller','exam.controller@rvce.edu.in','$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO', NULL, '11111111-1111-1111-1111-111111111099'),
    ('44444444-4444-4444-4444-444444444903', 'Seed Teacher',       'teacher@rvce.edu.in',        '$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO', NULL, '11111111-1111-1111-1111-111111111001'),
    ('44444444-4444-4444-4444-444444444904', 'Seed Student',       'student@rvce.edu.in',        '$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO', '1RV23CS999', '11111111-1111-1111-1111-111111111001'),
    ('44444444-4444-4444-4444-444444444905', 'Seed Another',       'another@rvce.edu.in',        '$2b$12$0dsNhVCAEAcS3nMYQnoc0.WvWNfo27XJWooMCvyL.B2bquUuJOkpO', NULL, '11111111-1111-1111-1111-111111111099')
ON CONFLICT (email) DO NOTHING;

-- Assign best-fit roles to the seeded deployment users.
INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.user_id, r.role_id, NULL
FROM users u
JOIN roles r ON LOWER(r.name) = 'admin'
WHERE LOWER(u.email) = 'admin@rvce.edu.in'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.user_id, r.role_id, NULL
FROM users u
JOIN roles r ON LOWER(r.name) = 'exam_controller'
WHERE LOWER(u.email) = 'exam.controller@rvce.edu.in'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.user_id, r.role_id, NULL
FROM users u
JOIN roles r ON LOWER(r.name) = 'teacher'
WHERE LOWER(u.email) = 'teacher@rvce.edu.in'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.user_id, r.role_id, NULL
FROM users u
JOIN roles r ON LOWER(r.name) = 'student'
WHERE LOWER(u.email) = 'student@rvce.edu.in'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.user_id, r.role_id, NULL
FROM users u
JOIN roles r ON LOWER(r.name) = 'another'
WHERE LOWER(u.email) = 'another@rvce.edu.in'
ON CONFLICT DO NOTHING;
