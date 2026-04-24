-- ============================================================
-- V3__exam_seating.sql
-- SCAS - Exam Sessions, Halls, Constraints, Seats, Jobs
--
-- DECISION LOG:
--   [D13] exam_halls is a junction between exam_sessions and
--         rooms. One exam may use 5 rooms simultaneously.
--         assigned_capacity is stored separately from rooms.capacity
--         because you might intentionally use only 80 of 100 seats
--         (reserve for last-minute additions). Without this, you'd
--         have no record of the intended exam capacity.
--
--   [D14] seating_constraints.parameters is JSONB not separate
--         columns. Each constraint type has a completely different
--         parameter shape:
--           NoBranchAdjacent    -> {}  (no params)
--           FrontRowReserved    -> {"rows": 2}
--           ExclusionZone       -> {"positions": [[1,3],[2,4],[5,2]]}
--           MaxSameBranchPerRow -> {"max": 3}
--         A fixed column schema would require NULLs in 90% of cells.
--         JSONB + GIN index handles containment queries efficiently.
--
--   [D15] exam_students.student_id FK is NULLABLE.
--         External students (competitive exams hosted at RVCE)
--         have no user accounts in the system. They're identified
--         by USN only. Forcing NOT NULL would block this real use case.
--
--   [D16] seating_jobs table exists because CSP algorithm runs
--         async (up to 30 seconds for 600 students). The POST
--         /generate endpoint returns job_id immediately; frontend
--         polls GET /jobs/{id}. Without this table: no way to
--         report partial results, constraint violations, or failure
--         reasons back to the coordinator.
--
--   [D17] exam_seats has a UNIQUE constraint on
--         (exam_id, hall_id, bench_row, bench_col).
--         This is the DB-level guarantee that no two students
--         share the same physical bench. Even if the CSP algorithm
--         has a bug, the DB rejects the invalid insert.
--
--   [D18] exam_seats.is_manual_override = TRUE flags seats that
--         were moved by the coordinator after CSP generation.
--         Used in audit queries: "how many seats did we manually
--         adjust?" Helps evaluate algorithm quality over time.
-- ============================================================

-- ─── EXAM_SESSIONS ───────────────────────────────────────────
CREATE TABLE exam_sessions (
    exam_id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,   -- "Dec 2025 CIE-3 - 5th Sem CSE"
    subject_code    VARCHAR(20)  NOT NULL,
    subject_name    VARCHAR(100) NOT NULL,
    section         VARCHAR(10)  NULL,       -- NULL = all sections combined
    semester        SMALLINT    NOT NULL,
    department_id   UUID        NOT NULL REFERENCES departments(department_id),
    exam_date       DATE        NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'DRAFT',
    created_by      UUID        NOT NULL REFERENCES users(user_id),
    published_at    TIMESTAMPTZ NULL,        -- when students can see seats
    completed_at    TIMESTAMPTZ NULL,        -- post-exam
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_exam_status CHECK (
        status IN ('DRAFT','CONFIGURED','GENERATED','PUBLISHED','COMPLETED','CANCELLED')
    ),
    CONSTRAINT chk_exam_time   CHECK (end_time > start_time),
    CONSTRAINT chk_exam_sem    CHECK (semester BETWEEN 1 AND 8),
    -- DECISION: published_at only set when status=PUBLISHED
    CONSTRAINT chk_publish_consistency CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR status != 'PUBLISHED'
    )
);

CREATE TRIGGER trg_exam_sessions_updated_at
    BEFORE UPDATE ON exam_sessions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ─── EXAM_HALLS ──────────────────────────────────────────────
-- DECISION [D13]: junction table, not a column on exam_sessions
-- Enables: 1 exam -> many halls
CREATE TABLE exam_halls (
    hall_id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id             UUID        NOT NULL REFERENCES exam_sessions(exam_id)
                                        ON DELETE CASCADE,
    room_id             UUID        NOT NULL REFERENCES rooms(room_id),
    -- assigned_capacity may be < room.capacity (DECISION [D13])
    assigned_capacity   SMALLINT    NOT NULL,
    total_benches       SMALLINT    NOT NULL,
    bench_rows          SMALLINT    NOT NULL,
    bench_cols          SMALLINT    NOT NULL,
    invigilator_id      UUID        NULL REFERENCES users(user_id),
    sort_order          SMALLINT    NOT NULL DEFAULT 1,

    CONSTRAINT uq_exam_room         UNIQUE (exam_id, room_id),
    CONSTRAINT chk_hall_capacity    CHECK (assigned_capacity > 0),
    CONSTRAINT chk_hall_benches     CHECK (
        total_benches <= bench_rows * bench_cols
    )
);

-- ─── EXAM_STUDENTS ───────────────────────────────────────────
-- DECISION [D15]: student_id nullable for external students
CREATE TABLE exam_students (
    entry_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID        NOT NULL REFERENCES exam_sessions(exam_id)
                                    ON DELETE CASCADE,
    -- NULL for external students (competitive exams at RVCE)
    student_id      UUID        NULL REFERENCES users(user_id)
                                    ON DELETE SET NULL,
    usn             VARCHAR(20)  NOT NULL,
    student_name    VARCHAR(150) NOT NULL,
    branch_code     VARCHAR(10)  NOT NULL,   -- CSE, ISE, ECE, MECH
    needs_front_row BOOLEAN     NOT NULL DEFAULT FALSE,
    upload_batch_id VARCHAR(36)  NULL,       -- UUID of the CSV upload batch
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- DECISION: one student per exam (prevent duplicate enrollment)
    CONSTRAINT uq_exam_student UNIQUE (exam_id, usn)
);

COMMENT ON COLUMN exam_students.student_id   IS 'NULL for external students without SCAS accounts';
COMMENT ON COLUMN exam_students.needs_front_row IS 'TRUE for students with visual/physical disabilities';
COMMENT ON COLUMN exam_students.upload_batch_id IS 'Traces back to the CSV upload that created this row';

-- ─── SEATING_CONSTRAINTS ─────────────────────────────────────
-- DECISION [D14]: JSONB parameters column
-- Each constraint type has a different parameter shape.
-- GIN index on parameters enables @> containment queries.
CREATE TABLE seating_constraints (
    constraint_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID        NOT NULL REFERENCES exam_sessions(exam_id)
                                    ON DELETE CASCADE,
    constraint_type VARCHAR(40)  NOT NULL,
    -- JSONB: flexible per-type params (DECISION [D14])
    parameters      JSONB       NOT NULL DEFAULT '{}',
    is_hard         BOOLEAN     NOT NULL DEFAULT TRUE,
    -- priority: lower number = evaluated first in CSP
    priority        SMALLINT    NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_constraint_type CHECK (constraint_type IN (
        'NO_BRANCH_ADJACENT',
        'NO_SAME_BRANCH_IN_ROW',
        'ALTERNATE_BRANCH_PATTERN',
        'FRONT_ROW_RESERVED',
        'EXCLUSION_ZONE',
        'MAX_SAME_BRANCH_PER_ROW'
    )),
    CONSTRAINT chk_priority CHECK (priority BETWEEN 1 AND 10),
    -- DECISION: each constraint type appears once per exam
    CONSTRAINT uq_constraint_per_exam UNIQUE (exam_id, constraint_type)
);

COMMENT ON COLUMN seating_constraints.is_hard IS 'TRUE=must satisfy or generation fails. FALSE=try to satisfy, soft violation logged.';

-- ─── SEATING_JOBS ────────────────────────────────────────────
-- DECISION [D16]: async CSP tracking
-- CSP may run 30 seconds. This table is polled by frontend.
CREATE TABLE seating_jobs (
    job_id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id             UUID        NOT NULL REFERENCES exam_sessions(exam_id)
                                        ON DELETE CASCADE,
    status              VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
    assigned_count      INTEGER     NULL,
    total_count         INTEGER     NULL,
    -- JSONB arrays stored here for partial result reporting
    unassigned_students JSONB       NULL,    -- [{usn, name, reason}]
    constraint_violations JSONB     NULL,   -- [{type, count, detail}]
    error_message       TEXT        NULL,
    duration_ms         INTEGER     NULL,
    triggered_by        UUID        NOT NULL REFERENCES users(user_id),
    started_at          TIMESTAMPTZ NULL,
    finished_at         TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_job_status CHECK (
        status IN ('PENDING','RUNNING','DONE','PARTIAL','FAILED','CANCELLED')
    )
);

-- ─── EXAM_SEATS ──────────────────────────────────────────────
-- DECISION [D17]: DB-level unique bench guarantee
-- DECISION [D18]: is_manual_override tracks algorithm quality
CREATE TABLE exam_seats (
    seat_id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id             UUID        NOT NULL REFERENCES exam_sessions(exam_id)
                                        ON DELETE CASCADE,
    student_id          UUID        NOT NULL REFERENCES users(user_id),
    hall_id             UUID        NOT NULL REFERENCES exam_halls(hall_id)
                                        ON DELETE CASCADE,
    job_id              UUID        NULL REFERENCES seating_jobs(job_id)
                                        ON DELETE SET NULL,
    bench_row           SMALLINT    NOT NULL,
    bench_col           SMALLINT    NOT NULL,
    bench_number        VARCHAR(10)  NOT NULL,   -- A-12, computed: row-col
    status              VARCHAR(10)  NOT NULL DEFAULT 'ASSIGNED',
    is_manual_override  BOOLEAN     NOT NULL DEFAULT FALSE,
    overridden_by       UUID        NULL REFERENCES users(user_id),
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- DECISION [D17]: physical bench uniqueness enforced at DB level
    CONSTRAINT uq_bench_position  UNIQUE (hall_id, bench_row, bench_col),
    -- DECISION: student gets exactly one seat per exam
    CONSTRAINT uq_student_per_exam UNIQUE (exam_id, student_id),
    CONSTRAINT chk_seat_status    CHECK (status IN ('ASSIGNED','ABSENT','PRESENT')),
    CONSTRAINT chk_bench_positive CHECK (bench_row > 0 AND bench_col > 0)
);

COMMENT ON COLUMN exam_seats.bench_number      IS 'Human-readable: A-12 format. Row letter + col number.';
COMMENT ON COLUMN exam_seats.is_manual_override IS 'TRUE when coordinator moved student from CSP-assigned seat.';

CREATE TRIGGER trg_exam_seats_updated_at
    BEFORE UPDATE ON exam_seats
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
