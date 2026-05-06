# Database Schema (extracted from migrations)

## departments
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `department_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `name` | VARCHAR(100) NOT NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_dept_code UNIQUE (code) |
| `CONSTRAINT` | uq_dept_name UNIQUE (name) |

**Table constraints / indexes**:

- code            VARCHAR(10)  NOT NULL,   -- CSE, ISE, ECE, MECH...
- -- DECISION: hod_user_id set after users exist, so deferred FK
- hod_user_id     UUID        NULL,        -- set after user rows exist

---

## users
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `user_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `name` | VARCHAR(150) NOT NULL |
| `email` | VARCHAR(255) NOT NULL |
| `department_id` | UUID        NULL REFERENCES departments(department_id) |
| `ON` | DELETE SET NULL |
| `is_active` | BOOLEAN     NOT NULL DEFAULT TRUE |
| `failed_login_count` | SMALLINT NOT NULL DEFAULT 0 |
| `last_login_at` | TIMESTAMPTZ NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_users_email UNIQUE (email) |
| `CONSTRAINT` | uq_users_usn   UNIQUE (usn) |
| `CONSTRAINT` | chk_usn_format CHECK ( |
| `CONSTRAINT` | chk_failed_logins CHECK (failed_login_count >= 0) |

**Table constraints / indexes**:

- password_hash   VARCHAR(72) NOT NULL,    -- BCrypt output always ~60 chars
- usn             VARCHAR(20)  NULL,       -- 1RV21CS001 format; NULL for staff
- locked_until    TIMESTAMPTZ NULL,       -- NULL = not locked
- usn IS NULL OR usn ~ '^[0-9A-Z]{10,13}$'
- )

---

## roles
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `role_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `name` | VARCHAR(50)  NOT NULL |
| `description` | TEXT        NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_role_name UNIQUE (name) |
| `CONSTRAINT` | chk_role_name CHECK ( |

**Table constraints / indexes**:

- -- DECISION: CHECK constraint documents valid values at DB level.
- -- Spring RBAC also enforces this, but DB is the last line of defence.
- name IN ('STUDENT','TEACHER','TTO','DEPT_COORD','ADMIN')
- )

---

## permissions
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `permission_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `description` | TEXT        NULL |
| `CONSTRAINT` | chk_perm_resource CHECK ( |
| `CONSTRAINT` | chk_perm_action CHECK ( |

**Table constraints / indexes**:

- resource        VARCHAR(50)  NOT NULL,  -- rooms, timetable, exam, reports
- action          VARCHAR(20)  NOT NULL,  -- read, write, delete, publish
- CONSTRAINT uq_perm UNIQUE (resource, action)
- resource IN ('rooms','timetable','exam','reports','notifications','audit','users')
- )
- action IN ('read','write','delete','publish','verify','generate')
- )

---

## user_roles
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `user_id` | UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE |
| `role_id` | UUID        NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE |
| `granted_by` | UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL |
| `granted_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |

**Table constraints / indexes**:

- expires_at      TIMESTAMPTZ NULL,   -- NULL = permanent
- CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id)

---

## role_permissions
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `role_id` | UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE |
| `permission_id` | UUID NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE |

**Table constraints / indexes**:

- CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id)

---

## refresh_tokens
*Source: V1__users_roles.sql*

| Column | Type & Constraints |
|---|---|
| `token_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `user_id` | UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE |
| `ip_address` | INET        NULL |
| `user_agent` | TEXT        NULL |
| `is_revoked` | BOOLEAN     NOT NULL DEFAULT FALSE |
| `expires_at` | TIMESTAMPTZ NOT NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_token_hash UNIQUE (token_hash) |

**Table constraints / indexes**:

- token_hash      VARCHAR(64) NOT NULL,   -- hex of SHA-256

---

## rooms
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `room_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `room_type` | VARCHAR(20)  NOT NULL |
| `capacity` | INTEGER     NOT NULL |
| `bench_rows` | INTEGER     NULL |
| `bench_cols` | INTEGER     NULL |
| `floor_number` | INTEGER     NOT NULL DEFAULT 0 |
| `dept_owner_id` | UUID        NULL REFERENCES departments(department_id) |
| `ON` | DELETE SET NULL |
| `is_active` | BOOLEAN     NOT NULL DEFAULT TRUE |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_room_name       UNIQUE (name) |
| `CONSTRAINT` | chk_room_type      CHECK (room_type IN ( |
| `CONSTRAINT` | chk_capacity       CHECK (capacity > 0 AND capacity <= 500) |
| `CONSTRAINT` | chk_bench_positive CHECK ( |
| `OR` | (bench_rows > 0 AND bench_cols > 0) |
| `CONSTRAINT` | chk_floor          CHECK (floor_number >= 0 AND floor_number <= 10) |

**Table constraints / indexes**:

- name            VARCHAR(20)  NOT NULL,   -- A201, SH-01, LAB-CS-3
- display_name    VARCHAR(100) NULL,       -- "Seminar Hall Block A"
- -- bench grid (NULL for non-exam rooms - DECISION [D7])
- block           VARCHAR(10)  NOT NULL,   -- A, B, C, D, Admin, Library
- building        VARCHAR(50)  NULL,       -- Main Block, PG Block
- -- GPS for campus map (NUMERIC not FLOAT - DECISION [D12])
- latitude        NUMERIC(10,7) NULL
- longitude       NUMERIC(10,7) NULL
- directions_text TEXT        NULL,        -- "From main gate, Block A, 2nd floor..."
- 'CLASSROOM','LAB','SEMINAR_HALL','EXAM_HALL','CONFERENCE_ROOM'
- ))
- (bench_rows IS NULL AND bench_cols IS NULL)
- )

---

## timetable_versions
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `version_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `status` | VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' |
| `created_by` | UUID        NOT NULL REFERENCES users(user_id) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_semester   CHECK (semester BETWEEN 1 AND 8) |
| `DEFERRABLE` | INITIALLY DEFERRED   -- allow transition: ACTIVE->ARCHIVED and DRAFT->ACTIVE in one txn |

**Table constraints / indexes**:

- academic_year   VARCHAR(10)  NOT NULL,   -- 2025-26
- semester        INTEGER     NOT NULL,    -- 1-8
- label           VARCHAR(100) NULL,       -- "Post-mid revision"
- activated_at    TIMESTAMPTZ NULL,        -- when status->ACTIVE
- archived_at     TIMESTAMPTZ NULL,        -- when status->ARCHIVED
- CONSTRAINT chk_tt_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED'))
- -- DECISION: only one ACTIVE version per semester per year
- -- Prevents ambiguity when querying "what is today's schedule?"
- CONSTRAINT uq_active_semester UNIQUE (academic_year, semester, status)

---

## timetable_slots
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `slot_id` | BIGSERIAL   PRIMARY KEY |
| `version_id` | UUID        NOT NULL REFERENCES timetable_versions(version_id) |
| `room_id` | UUID        NOT NULL REFERENCES rooms(room_id) |
| `teacher_id` | UUID        NOT NULL REFERENCES users(user_id) |
| `semester` | INTEGER     NOT NULL |
| `start_time` | TIME        NOT NULL |
| `end_time` | TIME        NOT NULL |
| `row_version` | INTEGER     NOT NULL DEFAULT 0 |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_slot_day    CHECK (day_of_week BETWEEN 1 AND 5) |
| `CONSTRAINT` | chk_slot_period CHECK (period_number BETWEEN 1 AND 8) |
| `CONSTRAINT` | chk_slot_time   CHECK (end_time > start_time) |

**Table constraints / indexes**:

- department      VARCHAR(100) NOT NULL,   -- Computer Science, Information Science
- subject_code    VARCHAR(20)  NOT NULL,   -- 21CS51
- subject_name    VARCHAR(100) NOT NULL,   -- Design & Analysis of Algorithms
- section         VARCHAR(5)   NOT NULL,   -- A, B, C, 1, 2
- day_of_week     INTEGER     NOT NULL,    -- 1=Monday, 5=Friday
- period_number   INTEGER     NOT NULL,    -- 1=8:00am, 8=5:00pm
- -- DECISION [D9]: optimistic locking column
- -- JPA @Version maps to this. Never set manually.
- -- DECISION: room cannot be double-booked in same version
- -- DB enforces this even if application validation is bypassed
- CONSTRAINT uq_slot_room_time UNIQUE (version_id, room_id, day_of_week, period_number)
- -- DECISION: teacher cannot be in two rooms simultaneously
- CONSTRAINT uq_slot_teacher_time UNIQUE (version_id, teacher_id, day_of_week, period_number)

---

## day_overrides
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `override_id` | BIGSERIAL   PRIMARY KEY |
| `slot_id` | BIGINT      NOT NULL REFERENCES timetable_slots(slot_id) |
| `ON` | DELETE CASCADE |
| `override_date` | DATE        NOT NULL |
| `status` | VARCHAR(15)  NOT NULL |
| `acting_teacher_id` | UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL |
| `reason` | TEXT        NULL |
| `source` | VARCHAR(10)  NOT NULL DEFAULT 'TEACHER' |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_override_status CHECK ( |
| `CONSTRAINT` | chk_override_source CHECK ( |

**Table constraints / indexes**:

- -- acting_teacher_id: who claimed this slot (NULL if just cancelled)
- -- DECISION: one override per slot per date
- -- Prevents: teacher cancels, then tries to cancel again same day
- CONSTRAINT uq_override_slot_date UNIQUE (slot_id, override_date)
- status IN ('CANCELLED','CLAIMED','EXTRA_CLASS')
- )
- source IN ('TEACHER','ADMIN','SYSTEM')
- )

---

## blackout_dates
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `blackout_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `blackout_date` | DATE        NOT NULL |
| `reason` | VARCHAR(100) NOT NULL |
| `scope` | VARCHAR(10)  NOT NULL DEFAULT 'GLOBAL' |
| `dept_id` | UUID        NULL REFERENCES departments(department_id) |
| `created_by` | UUID        NOT NULL REFERENCES users(user_id) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |

**Table constraints / indexes**:

- CONSTRAINT uq_blackout_date_dept UNIQUE (blackout_date, dept_id)
- CONSTRAINT chk_blackout_scope CHECK (scope IN ('GLOBAL','DEPT'))

---

## occupancy_records
*Source: V2__rooms_timetable.sql*

| Column | Type & Constraints |
|---|---|
| `record_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `room_id` | UUID        NOT NULL REFERENCES rooms(room_id) |
| `slot_id` | BIGINT      NULL REFERENCES timetable_slots(slot_id) |
| `ON` | DELETE SET NULL |
| `override_id` | BIGINT      NULL REFERENCES day_overrides(override_id) |
| `ON` | DELETE SET NULL |
| `check_date` | DATE        NOT NULL |
| `check_time` | TIME        NOT NULL |
| `method` | VARCHAR(15)  NOT NULL |
| `is_occupied` | BOOLEAN     NOT NULL |
| `verified_by` | UUID        NULL REFERENCES users(user_id) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_ai_confidence  CHECK (ai_confidence IS NULL OR |
| `CONSTRAINT` | chk_person_count   CHECK (person_count IS NULL OR person_count >= 0) |
| `CONSTRAINT` | chk_ai_has_confidence CHECK ( |
| `method` | != 'IMAGE_AI' OR ai_confidence IS NOT NULL |

**Table constraints / indexes**:

- -- NULL slot_id = ad-hoc check outside scheduled slot
- person_count    INTEGER     NULL,        -- from YOLO detection; NULL if MANUAL
- ai_confidence   NUMERIC(5,4) NULL,      -- 0.0000 to 1.0000
- image_url       TEXT        NULL,        -- MinIO object path
- CONSTRAINT chk_occ_method     CHECK (method IN ('MANUAL','IMAGE_AI'))
- (ai_confidence >= 0 AND ai_confidence <= 1))
- -- AI checks must have confidence score
- )

---

## exam_sessions
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `exam_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `subject_code` | VARCHAR(20)  NOT NULL |
| `subject_name` | VARCHAR(100) NOT NULL |
| `semester` | SMALLINT    NOT NULL |
| `department_id` | UUID        NOT NULL REFERENCES departments(department_id) |
| `exam_date` | DATE        NOT NULL |
| `start_time` | TIME        NOT NULL |
| `end_time` | TIME        NOT NULL |
| `status` | VARCHAR(15)  NOT NULL DEFAULT 'DRAFT' |
| `created_by` | UUID        NOT NULL REFERENCES users(user_id) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_exam_status CHECK ( |
| `CONSTRAINT` | chk_exam_time   CHECK (end_time > start_time) |
| `CONSTRAINT` | chk_exam_sem    CHECK (semester BETWEEN 1 AND 8) |
| `CONSTRAINT` | chk_publish_consistency CHECK ( |
| `OR` | status != 'PUBLISHED' |

**Table constraints / indexes**:

- name            VARCHAR(150) NOT NULL,   -- "Dec 2025 CIE-3 - 5th Sem CSE"
- section         VARCHAR(10)  NULL,       -- NULL = all sections combined
- published_at    TIMESTAMPTZ NULL,        -- when students can see seats
- completed_at    TIMESTAMPTZ NULL,        -- post-exam
- status IN ('DRAFT','CONFIGURED','GENERATED','PUBLISHED','COMPLETED','CANCELLED')
- )
- -- DECISION: published_at only set when status=PUBLISHED
- (status = 'PUBLISHED' AND published_at IS NOT NULL)
- )

---

## exam_halls
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `hall_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `exam_id` | UUID        NOT NULL REFERENCES exam_sessions(exam_id) |
| `ON` | DELETE CASCADE |
| `room_id` | UUID        NOT NULL REFERENCES rooms(room_id) |
| `assigned_capacity` | SMALLINT    NOT NULL |
| `total_benches` | SMALLINT    NOT NULL |
| `bench_rows` | SMALLINT    NOT NULL |
| `bench_cols` | SMALLINT    NOT NULL |
| `invigilator_id` | UUID        NULL REFERENCES users(user_id) |
| `sort_order` | SMALLINT    NOT NULL DEFAULT 1 |
| `CONSTRAINT` | chk_hall_capacity    CHECK (assigned_capacity > 0) |
| `CONSTRAINT` | chk_hall_benches     CHECK ( |
| `total_benches` | <= bench_rows * bench_cols |

**Table constraints / indexes**:

- -- assigned_capacity may be < room.capacity (DECISION [D13])
- CONSTRAINT uq_exam_room         UNIQUE (exam_id, room_id)
- )

---

## exam_students
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `entry_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `exam_id` | UUID        NOT NULL REFERENCES exam_sessions(exam_id) |
| `ON` | DELETE CASCADE |
| `student_id` | UUID        NULL REFERENCES users(user_id) |
| `ON` | DELETE SET NULL |
| `usn` | VARCHAR(20)  NOT NULL |
| `student_name` | VARCHAR(150) NOT NULL |
| `needs_front_row` | BOOLEAN     NOT NULL DEFAULT FALSE |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |

**Table constraints / indexes**:

- -- NULL for external students (competitive exams at RVCE)
- branch_code     VARCHAR(10)  NOT NULL,   -- CSE, ISE, ECE, MECH
- upload_batch_id VARCHAR(36)  NULL,       -- UUID of the CSV upload batch
- -- DECISION: one student per exam (prevent duplicate enrollment)
- CONSTRAINT uq_exam_student UNIQUE (exam_id, usn)

---

## seating_constraints
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `constraint_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `exam_id` | UUID        NOT NULL REFERENCES exam_sessions(exam_id) |
| `ON` | DELETE CASCADE |
| `constraint_type` | VARCHAR(40)  NOT NULL |
| `parameters` | JSONB       NOT NULL DEFAULT '{}' |
| `is_hard` | BOOLEAN     NOT NULL DEFAULT TRUE |
| `priority` | SMALLINT    NOT NULL DEFAULT 1 |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_constraint_type CHECK (constraint_type IN ( |
| `CONSTRAINT` | chk_priority CHECK (priority BETWEEN 1 AND 10) |

**Table constraints / indexes**:

- -- JSONB: flexible per-type params (DECISION [D14])
- -- priority: lower number = evaluated first in CSP
- 'NO_BRANCH_ADJACENT'
- 'NO_SAME_BRANCH_IN_ROW'
- 'ALTERNATE_BRANCH_PATTERN'
- 'FRONT_ROW_RESERVED'
- 'EXCLUSION_ZONE'
- 'MAX_SAME_BRANCH_PER_ROW'
- ))
- -- DECISION: each constraint type appears once per exam
- CONSTRAINT uq_constraint_per_exam UNIQUE (exam_id, constraint_type)

---

## seating_jobs
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `job_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `exam_id` | UUID        NOT NULL REFERENCES exam_sessions(exam_id) |
| `ON` | DELETE CASCADE |
| `status` | VARCHAR(15)  NOT NULL DEFAULT 'PENDING' |
| `assigned_count` | INTEGER     NULL |
| `total_count` | INTEGER     NULL |
| `error_message` | TEXT        NULL |
| `duration_ms` | INTEGER     NULL |
| `triggered_by` | UUID        NOT NULL REFERENCES users(user_id) |
| `started_at` | TIMESTAMPTZ NULL |
| `finished_at` | TIMESTAMPTZ NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_job_status CHECK ( |

**Table constraints / indexes**:

- -- JSONB arrays stored here for partial result reporting
- unassigned_students JSONB       NULL,    -- [{usn, name, reason}]
- constraint_violations JSONB     NULL,   -- [{type, count, detail}]
- status IN ('PENDING','RUNNING','DONE','PARTIAL','FAILED','CANCELLED')
- )

---

## exam_seats
*Source: V3__exam_seating.sql*

| Column | Type & Constraints |
|---|---|
| `seat_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `exam_id` | UUID        NOT NULL REFERENCES exam_sessions(exam_id) |
| `ON` | DELETE CASCADE |
| `student_id` | UUID        NOT NULL REFERENCES users(user_id) |
| `hall_id` | UUID        NOT NULL REFERENCES exam_halls(hall_id) |
| `ON` | DELETE CASCADE |
| `job_id` | UUID        NULL REFERENCES seating_jobs(job_id) |
| `ON` | DELETE SET NULL |
| `bench_row` | SMALLINT    NOT NULL |
| `bench_col` | SMALLINT    NOT NULL |
| `status` | VARCHAR(10)  NOT NULL DEFAULT 'ASSIGNED' |
| `is_manual_override` | BOOLEAN     NOT NULL DEFAULT FALSE |
| `overridden_by` | UUID        NULL REFERENCES users(user_id) |
| `assigned_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_bench_positive CHECK (bench_row > 0 AND bench_col > 0) |

**Table constraints / indexes**:

- bench_number        VARCHAR(10)  NOT NULL,   -- A-12, computed: row-col
- -- DECISION [D17]: physical bench uniqueness enforced at DB level
- CONSTRAINT uq_bench_position  UNIQUE (hall_id, bench_row, bench_col)
- -- DECISION: student gets exactly one seat per exam
- CONSTRAINT uq_student_per_exam UNIQUE (exam_id, student_id)
- CONSTRAINT chk_seat_status    CHECK (status IN ('ASSIGNED','ABSENT','PRESENT'))

---

## fcm_device_tokens
*Source: V4__notifications_audit.sql*

| Column | Type & Constraints |
|---|---|
| `token_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `user_id` | UUID        NOT NULL REFERENCES users(user_id) |
| `ON` | DELETE CASCADE |
| `platform` | VARCHAR(10)  NOT NULL |
| `is_active` | BOOLEAN     NOT NULL DEFAULT TRUE |
| `last_seen_at` | TIMESTAMPTZ NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | uq_fcm_token  UNIQUE (fcm_token) |

**Table constraints / indexes**:

- fcm_token       TEXT        NOT NULL,    -- Firebase device token (varies in length)
- CONSTRAINT chk_platform  CHECK (platform IN ('WEB','ANDROID','IOS'))

---

## notification_batches
*Source: V4__notifications_audit.sql*

| Column | Type & Constraints |
|---|---|
| `batch_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `trigger_event` | VARCHAR(30)  NOT NULL |
| `triggered_by` | UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL |
| `ref_entity_id` | UUID        NULL |
| `total_recipients` | INTEGER     NOT NULL DEFAULT 0 |
| `sent_count` | INTEGER     NOT NULL DEFAULT 0 |
| `failed_count` | INTEGER     NOT NULL DEFAULT 0 |
| `status` | VARCHAR(15)  NOT NULL DEFAULT 'QUEUED' |
| `started_at` | TIMESTAMPTZ NULL |
| `completed_at` | TIMESTAMPTZ NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_batch_event CHECK (trigger_event IN ( |
| `CONSTRAINT` | chk_batch_status CHECK ( |
| `CONSTRAINT` | chk_counts_positive CHECK ( |
| `total_recipients` | >= 0 AND sent_count >= 0 AND failed_count >= 0 |

**Table constraints / indexes**:

- ref_entity_type     VARCHAR(20)  NULL,   -- EXAM, SLOT, TIMETABLE
- 'EXAM_PUBLISHED','SLOT_CANCELLED','ROOM_FREED'
- 'TIMETABLE_UPDATED','SYSTEM_ALERT'
- ))
- status IN ('QUEUED','PROCESSING','DONE','PARTIAL','FAILED')
- )
- )

---

## notifications
*Source: V4__notifications_audit.sql*

| Column | Type & Constraints |
|---|---|
| `notification_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `user_id` | UUID        NOT NULL REFERENCES users(user_id) |
| `ON` | DELETE CASCADE |
| `batch_id` | UUID        NULL REFERENCES notification_batches(batch_id) |
| `ON` | DELETE SET NULL |
| `notif_type` | VARCHAR(30)  NOT NULL |
| `title` | VARCHAR(200) NOT NULL |
| `body` | TEXT        NOT NULL |
| `payload` | JSONB       NOT NULL DEFAULT '{}' |
| `is_read` | BOOLEAN     NOT NULL DEFAULT FALSE |
| `delivery_status` | VARCHAR(10)  NOT NULL DEFAULT 'PENDING' |
| `fcm_message_id` | VARCHAR(200) NULL |
| `ref_entity_type` | VARCHAR(20)  NULL |
| `read_at` | TIMESTAMPTZ NULL |
| `sent_at` | TIMESTAMPTZ NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_notif_type CHECK (notif_type IN ( |
| `CONSTRAINT` | chk_delivery_status CHECK ( |
| `CONSTRAINT` | chk_read_consistency CHECK ( |
| `OR` | is_read = FALSE |

**Table constraints / indexes**:

- -- JSONB payload for frontend deep-linking (DECISION [D20])
- ref_entity_id   UUID        NULL,        -- exam_id or slot_id
- 'CLASS_CANCELLED','SEAT_PUBLISHED','ROOM_FREED'
- 'TIMETABLE_UPDATED','EXAM_REMINDER','SYSTEM'
- ))
- delivery_status IN ('PENDING','SENT','FAILED','READ')
- )
- -- read_at only set when is_read = TRUE
- (is_read = TRUE AND read_at IS NOT NULL)
- )

---

## reports
*Source: V4__notifications_audit.sql*

| Column | Type & Constraints |
|---|---|
| `report_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `report_type` | VARCHAR(25)  NOT NULL |
| `generated_by` | UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL |
| `parameters` | JSONB       NOT NULL DEFAULT '{}' |
| `file_format` | VARCHAR(10)  NOT NULL DEFAULT 'PDF' |
| `file_size_bytes` | BIGINT      NULL |
| `generation_ms` | INTEGER     NULL |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_report_type   CHECK (report_type IN ( |

**Table constraints / indexes**:

- -- Parameters used to generate (for cache key matching)
- file_url        TEXT        NULL,       -- MinIO presigned path (set after generation)
- expires_at      TIMESTAMPTZ NULL,       -- presigned URL expiry
- 'HALL_SEATING_PLAN','TIMETABLE','ROOM_AVAILABILITY'
- 'OCCUPANCY_SUMMARY','EXAM_ABSENTEE'
- ))
- CONSTRAINT chk_report_format CHECK (file_format IN ('PDF','EXCEL','CSV'))

---

## audit_logs
*Source: V4__notifications_audit.sql*

| Column | Type & Constraints |
|---|---|
| `log_id` | UUID        PRIMARY KEY DEFAULT gen_random_uuid() |
| `action` | VARCHAR(50)  NOT NULL |
| `entity_type` | VARCHAR(30)  NOT NULL |
| `ip_address` | INET        NULL |
| `user_agent` | TEXT        NULL |
| `success` | BOOLEAN     NOT NULL DEFAULT TRUE |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() |
| `CONSTRAINT` | chk_audit_action CHECK (action IN ( |
| `CONSTRAINT` | chk_audit_entity CHECK (entity_type IN ( |

**Table constraints / indexes**:

- -- NO FK - immutable (DECISION [D19])
- actor_id        UUID        NOT NULL,       -- user_id at time of action
- actor_email     VARCHAR(255) NOT NULL,      -- denormalised
- actor_role      VARCHAR(20)  NOT NULL,      -- denormalised
- entity_id       UUID        NULL,           -- NULL for login events
- old_value       JSONB       NULL,           -- state before change
- new_value       JSONB       NULL,           -- state after change
- error_message   TEXT        NULL,           -- set when success=FALSE
- 'LOGIN','LOGOUT','REGISTER'
- 'UPLOAD_TIMETABLE','ACTIVATE_TIMETABLE'
- 'CANCEL_SLOT','CLAIM_SLOT'
- 'GENERATE_SEATING','PUBLISH_EXAM','COMPLETE_EXAM'
- 'VERIFY_OCCUPANCY'
- 'SEND_NOTIFICATION'
- 'CREATE_USER','UPDATE_USER','DEACTIVATE_USER'
- 'CREATE_ROOM','UPDATE_ROOM','DEACTIVATE_ROOM'
- ))
- 'USER','ROOM','TIMETABLE_SLOT','DAY_OVERRIDE'
- 'EXAM_SESSION','EXAM_SEAT','NOTIFICATION','SYSTEM'
- ))

---

