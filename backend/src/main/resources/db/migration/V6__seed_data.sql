-- ============================================================
-- V6__seed_data.sql
-- SCAS - Seed Data: RVCE Rooms, Departments, Users, Roles
--
-- DECISION LOG:
--   [S1] Passwords are BCrypt hash of 'Test@1234' at strength 12.
--        Generated externally (Spring's BCryptPasswordEncoder).
--        NEVER store plain text. Never use MD5 or SHA.
--        All test accounts use SAME password for convenience;
--        production would require unique passwords + forced reset.
--
--   [S2] UUIDs are hard-coded (not gen_random_uuid()) in seed data.
--        Reason: other migration files or test fixtures may reference
--        these UUIDs. gen_random_uuid() would generate different UUIDs
--        on each environment, breaking cross-reference.
--
--   [S3] GPS coordinates are real RVCE coordinates (approximately).
--        Lat/lng for each block taken from Google Maps.
--        Stored as NUMERIC(10,7) for ~1cm precision.
--
--   [S4] INSERT...ON CONFLICT DO NOTHING makes seed idempotent.
--        Running './gradlew flywayMigrate' twice won't fail
--        because Flyway checksums V6 and won't re-run it.
--        But if someone manually runs this SQL, ON CONFLICT
--        prevents duplicate key violations.
-- ============================================================

-- ─── DEPARTMENTS ─────────────────────────────────────────────
INSERT INTO departments (department_id, name, code) VALUES
    ('11111111-1111-1111-1111-111111111001', 'Computer Science & Engineering',          'CSE'),
    ('11111111-1111-1111-1111-111111111002', 'Information Science & Engineering',       'ISE'),
    ('11111111-1111-1111-1111-111111111003', 'Electronics & Communication Engineering', 'ECE'),
    ('11111111-1111-1111-1111-111111111004', 'Mechanical Engineering',                  'MECH'),
    ('11111111-1111-1111-1111-111111111005', 'Civil Engineering',                       'CIVIL'),
    ('11111111-1111-1111-1111-111111111006', 'Electrical & Electronics Engineering',    'EEE'),
    ('11111111-1111-1111-1111-111111111099', 'Administration',                          'ADMIN')
ON CONFLICT (code) DO NOTHING;

-- ─── ROLES ───────────────────────────────────────────────────
INSERT INTO roles (role_id, name, description) VALUES
    ('22222222-2222-2222-2222-222222222001', 'STUDENT',    'Read-only: timetable view, exam seat view, notifications'),
    ('22222222-2222-2222-2222-222222222002', 'TEACHER',    'Cancel own classes, claim free slots, verify occupancy'),
    ('22222222-2222-2222-2222-222222222003', 'TTO',        'Upload and manage timetables, run timetable generator'),
    ('22222222-2222-2222-2222-222222222004', 'DEPT_COORD', 'Manage exam sessions, generate and publish seating plans'),
    ('22222222-2222-2222-2222-222222222005', 'ADMIN',      'Full system access: users, rooms, config, analytics')
ON CONFLICT (name) DO NOTHING;

-- ─── PERMISSIONS ─────────────────────────────────────────────
INSERT INTO permissions (permission_id, resource, action, description) VALUES
    ('33333333-3333-3333-3333-333333333001', 'rooms',          'read',     'View room availability'),
    ('33333333-3333-3333-3333-333333333002', 'rooms',          'write',    'Create/update rooms'),
    ('33333333-3333-3333-3333-333333333003', 'rooms',          'verify',   'Verify room occupancy'),
    ('33333333-3333-3333-3333-333333333004', 'timetable',      'read',     'View timetables'),
    ('33333333-3333-3333-3333-333333333005', 'timetable',      'write',    'Upload and modify timetables'),
    ('33333333-3333-3333-3333-333333333006', 'timetable',      'generate', 'Run timetable CSP generator'),
    ('33333333-3333-3333-3333-333333333007', 'exam',           'read',     'View exam seating'),
    ('33333333-3333-3333-3333-333333333008', 'exam',           'write',    'Create exam sessions and upload students'),
    ('33333333-3333-3333-3333-333333333009', 'exam',           'generate', 'Run CSP seating generation'),
    ('33333333-3333-3333-3333-333333333010', 'exam',           'publish',  'Publish seating plans to students'),
    ('33333333-3333-3333-3333-333333333011', 'reports',        'read',     'Download reports'),
    ('33333333-3333-3333-3333-333333333012', 'notifications',  'read',     'Receive notifications'),
    ('33333333-3333-3333-3333-333333333013', 'audit',          'read',     'View audit logs'),
    ('33333333-3333-3333-3333-333333333014', 'users',          'write',    'Create and manage users')
ON CONFLICT (resource, action) DO NOTHING;

-- ─── ROLE_PERMISSIONS mapping ─────────────────────────────────
-- STUDENT: read rooms, timetable, exam, notifications
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222001', '33333333-3333-3333-3333-333333333001'),
    ('22222222-2222-2222-2222-222222222001', '33333333-3333-3333-3333-333333333004'),
    ('22222222-2222-2222-2222-222222222001', '33333333-3333-3333-3333-333333333007'),
    ('22222222-2222-2222-2222-222222222001', '33333333-3333-3333-3333-333333333012')
ON CONFLICT DO NOTHING;

-- TEACHER: all student perms + verify occupancy
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333001'),
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333003'),
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333004'),
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333007'),
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333011'),
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333012')
ON CONFLICT DO NOTHING;

-- TTO: timetable write + generate
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333001'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333004'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333005'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333006'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333011'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333012')
ON CONFLICT DO NOTHING;

-- DEPT_COORD: exam full access
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333001'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333004'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333007'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333008'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333009'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333010'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333011'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333012')
ON CONFLICT DO NOTHING;

-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
    SELECT '22222222-2222-2222-2222-222222222005', permission_id FROM permissions
ON CONFLICT DO NOTHING;

-- ─── USERS (1 per role, password = BCrypt('Test@1234', 12)) ───
-- DECISION [S1]: BCrypt hash generated with Spring's BCryptPasswordEncoder.encode("Test@1234")
-- Hash always starts with $2a$12$ (BCrypt algorithm + strength 12)
INSERT INTO users (user_id, name, email, password_hash, usn, department_id) VALUES
    ('44444444-4444-4444-4444-444444444001',
     'Admin User',
     'admin@rvce.edu.in',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8d1pCfHvHBpwMX5CdZy',
     NULL,
     '11111111-1111-1111-1111-111111111099'),

    ('44444444-4444-4444-4444-444444444002',
     'TTO Officer',
     'tto@rvce.edu.in',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8d1pCfHvHBpwMX5CdZy',
     NULL,
     '11111111-1111-1111-1111-111111111099'),

    ('44444444-4444-4444-4444-444444444003',
     'Dr. Priya Sharma',
     'priya.sharma@rvce.edu.in',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8d1pCfHvHBpwMX5CdZy',
     NULL,
     '11111111-1111-1111-1111-111111111001'),

    ('44444444-4444-4444-4444-444444444004',
     'Dr. Ramesh Kumar',
     'ramesh.kumar@rvce.edu.in',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8d1pCfHvHBpwMX5CdZy',
     NULL,
     '11111111-1111-1111-1111-111111111001'),

    ('44444444-4444-4444-4444-444444444005',
     'Kiran Reddy',
     'kiran@rvce.edu.in',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8d1pCfHvHBpwMX5CdZy',
     '1RV22CS050',
     '11111111-1111-1111-1111-111111111001')
ON CONFLICT (email) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role_id, granted_by) VALUES
    ('44444444-4444-4444-4444-444444444001', '22222222-2222-2222-2222-222222222005', '44444444-4444-4444-4444-444444444001'),
    ('44444444-4444-4444-4444-444444444002', '22222222-2222-2222-2222-222222222003', '44444444-4444-4444-4444-444444444001'),
    ('44444444-4444-4444-4444-444444444003', '22222222-2222-2222-2222-222222222004', '44444444-4444-4444-4444-444444444001'),
    ('44444444-4444-4444-4444-444444444004', '22222222-2222-2222-2222-222222222002', '44444444-4444-4444-4444-444444444001'),
    ('44444444-4444-4444-4444-444444444005', '22222222-2222-2222-2222-222222222001', '44444444-4444-4444-4444-444444444001')
ON CONFLICT DO NOTHING;

-- Update HODs now that users exist (DEFERRABLE FK)
UPDATE departments SET hod_user_id = '44444444-4444-4444-4444-444444444003'
    WHERE code = 'CSE';

-- ─── 30 RVCE ROOMS ─────────────────────────────────────────────
-- GPS coordinates are approximate for RVCE Bengaluru (~12.9237, 77.4994)
-- DECISION [S3]: Real coordinates enable GPS navigation on campus map.
INSERT INTO rooms (
    room_id, name, display_name, room_type, capacity,
    bench_rows, bench_cols, floor_number, block, building,
    latitude, longitude, directions_text, dept_owner_id
) VALUES
-- ── Block A: Classrooms ────────────────────────────────────────
('55555555-5555-5555-5555-555555555001','A101','Classroom A101','CLASSROOM',60,NULL,NULL,1,'A','Main Block',
 12.9232100,77.4991200,'From main gate, enter Main Block, Ground floor, first corridor left.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555002','A102','Classroom A102','CLASSROOM',60,NULL,NULL,1,'A','Main Block',
 12.9232100,77.4991500,'From main gate, enter Main Block, Ground floor, second door left.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555003','A201','Classroom A201','CLASSROOM',60,NULL,NULL,2,'A','Main Block',
 12.9232400,77.4991200,'Main Block Block A, take stairs to Floor 1, turn right, third room.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555004','A202','Classroom A202','CLASSROOM',60,NULL,NULL,2,'A','Main Block',
 12.9232400,77.4991500,'Main Block Block A, Floor 1, fourth room on right.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555005','A301','Classroom A301','CLASSROOM',60,NULL,NULL,3,'A','Main Block',
 12.9232700,77.4991200,'Main Block Block A, Floor 2, first room on right.',
 '11111111-1111-1111-1111-111111111002'),

('55555555-5555-5555-5555-555555555006','A302','Classroom A302','CLASSROOM',60,NULL,NULL,3,'A','Main Block',
 12.9232700,77.4991500,'Main Block Block A, Floor 2, second room on right.',
 '11111111-1111-1111-1111-111111111002'),

-- ── Block B: Labs ──────────────────────────────────────────────
('55555555-5555-5555-5555-555555555007','B101','CSE Lab 1','LAB',40,NULL,NULL,1,'B','Main Block',
 12.9233000,77.4993000,'Enter Block B from left corridor, Ground floor, Lab 1 on right.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555008','B102','CSE Lab 2','LAB',40,NULL,NULL,1,'B','Main Block',
 12.9233000,77.4993500,'Block B Ground floor, Lab 2 opposite Lab 1.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555009','B201','ISE Lab 1','LAB',40,NULL,NULL,2,'B','Main Block',
 12.9233300,77.4993000,'Block B Floor 1, ISE Lab 1.',
 '11111111-1111-1111-1111-111111111002'),

('55555555-5555-5555-5555-555555555010','B202','ECE Lab 1','LAB',40,NULL,NULL,2,'B','Main Block',
 12.9233300,77.4993500,'Block B Floor 1, ECE Lab 1.',
 '11111111-1111-1111-1111-111111111003'),

-- ── Block C: Classrooms ────────────────────────────────────────
('55555555-5555-5555-5555-555555555011','C101','Classroom C101','CLASSROOM',70,NULL,NULL,1,'C','New Block',
 12.9234000,77.4995000,'New Block entrance, Ground floor, C-wing first door.',
 '11111111-1111-1111-1111-111111111003'),

('55555555-5555-5555-5555-555555555012','C102','Classroom C102','CLASSROOM',70,NULL,NULL,1,'C','New Block',
 12.9234000,77.4995500,'New Block Ground floor, second door C-wing.',
 '11111111-1111-1111-1111-111111111003'),

('55555555-5555-5555-5555-555555555013','C201','Classroom C201','CLASSROOM',70,NULL,NULL,2,'C','New Block',
 12.9234300,77.4995000,'New Block Floor 1, C-wing first door.',
 '11111111-1111-1111-1111-111111111004'),

('55555555-5555-5555-5555-555555555014','C202','Classroom C202','CLASSROOM',70,NULL,NULL,2,'C','New Block',
 12.9234300,77.4995500,'New Block Floor 1, C-wing second door.',
 '11111111-1111-1111-1111-111111111004'),

('55555555-5555-5555-5555-555555555015','C301','Classroom C301','CLASSROOM',70,NULL,NULL,3,'C','New Block',
 12.9234600,77.4995000,'New Block Floor 2, C-wing first door.',
 '11111111-1111-1111-1111-111111111005'),

-- ── Block D: Exam Halls (bench grid set) ──────────────────────
('55555555-5555-5555-5555-555555555016','D101','Exam Hall D101','EXAM_HALL',120,10,12,1,'D','Examination Block',
 12.9235000,77.4997000,'Examination Block, Ground floor, Hall D101.',
 NULL),

('55555555-5555-5555-5555-555555555017','D102','Exam Hall D102','EXAM_HALL',120,10,12,1,'D','Examination Block',
 12.9235000,77.4997500,'Examination Block, Ground floor, Hall D102.',
 NULL),

('55555555-5555-5555-5555-555555555018','D201','Exam Hall D201','EXAM_HALL',80,8,10,2,'D','Examination Block',
 12.9235300,77.4997000,'Examination Block, Floor 1, Hall D201.',
 NULL),

('55555555-5555-5555-5555-555555555019','D202','Exam Hall D202','EXAM_HALL',80,8,10,2,'D','Examination Block',
 12.9235300,77.4997500,'Examination Block, Floor 1, Hall D202.',
 NULL),

('55555555-5555-5555-5555-555555555020','D301','Exam Hall D301','EXAM_HALL',60,6,10,3,'D','Examination Block',
 12.9235600,77.4997000,'Examination Block, Floor 2, Hall D301.',
 NULL),

-- ── Seminar Halls ──────────────────────────────────────────────
('55555555-5555-5555-5555-555555555021','SH-A','Seminar Hall A','SEMINAR_HALL',150,NULL,NULL,1,'A','Main Block',
 12.9232800,77.4990000,'Main Block, Ground floor, follow signboards to Seminar Hall A.',
 NULL),

('55555555-5555-5555-5555-555555555022','SH-B','Seminar Hall B','SEMINAR_HALL',100,NULL,NULL,2,'B','Main Block',
 12.9233500,77.4993800,'Block B, Floor 1, Seminar Hall B opposite staircase.',
 NULL),

('55555555-5555-5555-5555-555555555023','SH-C','Seminar Hall C','SEMINAR_HALL',80,NULL,NULL,1,'C','New Block',
 12.9234100,77.4996000,'New Block, Ground floor, Seminar Hall C near elevator.',
 NULL),

-- ── Conference Rooms ───────────────────────────────────────────
('55555555-5555-5555-5555-555555555024','CONF-1','Conference Room 1','CONFERENCE_ROOM',20,NULL,NULL,3,'A','Main Block',
 12.9233000,77.4991800,'Block A, Floor 2, Conference Room 1 near HOD office.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555025','CONF-2','Conference Room 2','CONFERENCE_ROOM',20,NULL,NULL,3,'B','Main Block',
 12.9233800,77.4994200,'Block B, Floor 2, Conference Room 2.',
 '11111111-1111-1111-1111-111111111002'),

-- ── More Classrooms ────────────────────────────────────────────
('55555555-5555-5555-5555-555555555026','A401','Classroom A401','CLASSROOM',60,NULL,NULL,4,'A','Main Block',
 12.9233000,77.4991000,'Block A, Floor 3, Room A401.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555027','A402','Classroom A402','CLASSROOM',60,NULL,NULL,4,'A','Main Block',
 12.9233000,77.4991300,'Block A, Floor 3, Room A402.',
 '11111111-1111-1111-1111-111111111002'),

('55555555-5555-5555-5555-555555555028','B301','CSE Lab 3','LAB',40,NULL,NULL,3,'B','Main Block',
 12.9233600,77.4993200,'Block B, Floor 2, CSE Lab 3.',
 '11111111-1111-1111-1111-111111111001'),

('55555555-5555-5555-5555-555555555029','C401','Classroom C401','CLASSROOM',70,NULL,NULL,4,'C','New Block',
 12.9234900,77.4995200,'New Block, Floor 3, Room C401.',
 '11111111-1111-1111-1111-111111111005'),

('55555555-5555-5555-5555-555555555030','D401','Exam Hall D401','EXAM_HALL',100,10,10,4,'D','Examination Block',
 12.9235900,77.4997200,'Examination Block, Floor 3, Hall D401.',
 NULL)
ON CONFLICT (name) DO NOTHING;

-- ─── TIMETABLE VERSION ────────────────────────────────────────
INSERT INTO timetable_versions (
    version_id, academic_year, semester, label, status, created_by
) VALUES (
    '66666666-6666-6666-6666-666666666001',
    '2025-26', 5, 'Odd Semester 2025-26 - Initial', 'ACTIVE',
    '44444444-4444-4444-4444-444444444002'
) ON CONFLICT (version_id) DO NOTHING;

-- ─── SAMPLE TIMETABLE SLOTS ───────────────────────────────────
INSERT INTO timetable_slots (
    slot_id, version_id, room_id, teacher_id, department_id,
    subject_code, subject_name, section, semester,
    day_of_week, period_number, start_time, end_time
) VALUES
-- Monday period 1: DAA in A201 by Dr. Ramesh
('77777777-7777-7777-7777-777777777001',
 '66666666-6666-6666-6666-666666666001',
 '55555555-5555-5555-5555-555555555003',
 '44444444-4444-4444-4444-444444444004',
 '11111111-1111-1111-1111-111111111001',
 '21CS51','Design & Analysis of Algorithms','A',5,
 1,1,'08:00','09:00'),

-- Monday period 2: OS in A202 by Dr. Ramesh
('77777777-7777-7777-7777-777777777002',
 '66666666-6666-6666-6666-666666666001',
 '55555555-5555-5555-5555-555555555004',
 '44444444-4444-4444-4444-444444444004',
 '11111111-1111-1111-1111-111111111001',
 '21CS52','Operating Systems','B',5,
 1,2,'09:00','10:00'),

-- Wednesday period 3: CN Lab in B101 by Dr. Priya
('77777777-7777-7777-7777-777777777003',
 '66666666-6666-6666-6666-666666666001',
 '55555555-5555-5555-5555-555555555007',
 '44444444-4444-4444-4444-444444444003',
 '11111111-1111-1111-1111-111111111001',
 '21CSL57','CN & OS Lab','A',5,
 3,3,'10:15','12:15')
ON CONFLICT DO NOTHING;

-- ─── VERIFY SEED DATA ─────────────────────────────────────────
DO $$
DECLARE
    v_room_count    INTEGER;
    v_user_count    INTEGER;
    v_role_count    INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_room_count FROM rooms;
    SELECT COUNT(*) INTO v_user_count FROM users;
    SELECT COUNT(*) INTO v_role_count FROM roles;

    IF v_room_count < 30 THEN
        RAISE EXCEPTION 'Seed verification failed: expected 30 rooms, found %', v_room_count;
    END IF;
    IF v_user_count < 5 THEN
        RAISE EXCEPTION 'Seed verification failed: expected 5 users, found %', v_user_count;
    END IF;
    IF v_role_count < 5 THEN
        RAISE EXCEPTION 'Seed verification failed: expected 5 roles, found %', v_role_count;
    END IF;

    RAISE NOTICE 'Seed verification passed: % rooms, % users, % roles', v_room_count, v_user_count, v_role_count;
END $$;
