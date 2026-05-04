-- ============================================================
-- V2__rooms_timetable.sql
-- SCAS - Rooms, Timetable Slots, Overrides, Occupancy
--
-- DECISION LOG:
--   [D7] rooms.bench_rows/bench_cols NULL for non-exam rooms.
--        Only exam halls need a bench grid. Storing zeroes
--        would pass CHECK constraints but mislead readers.
--        NULL explicitly communicates "not applicable."
--
--   [D8] timetable_versions table exists so that when a new
--        semester timetable is uploaded, old slots are NOT
--        deleted. Set new version status=ACTIVE, old=ARCHIVED.
--        This gives instant rollback (flip status) and full
--        scheduling history without complex migration scripts.
--
--   [D9] timetable_slots.row_version (INTEGER) for optimistic
--        locking. When two teachers simultaneously try to claim
--        the same freed slot, JPA includes row_version in the
--        UPDATE WHERE clause. Second writer's version doesn't
--        match -> OptimisticLockException -> clean 409 response.
--        Alternative (pessimistic SELECT FOR UPDATE) blocks the
--        second connection thread entirely - bad under load.
--
--   [D10] day_overrides is SEPARATE from timetable_slots.
--         timetable_slots = immutable canonical schedule.
--         day_overrides = ephemeral day-level events.
--         Keeps the schedule auditable and rollback-safe.
--         A cancelled class doesn't touch the master timetable.
--
--   [D11] occupancy_records.slot_id is NULLABLE.
--         Teachers can check if a room is empty OUTSIDE a
--         scheduled slot (ad-hoc check). Forcing NOT NULL would
--         block this valid use case entirely.
--
--   [D12] latitude/longitude stored as NUMERIC(10,7) not FLOAT.
--         FLOAT has representation errors (0.1+0.2 != 0.3).
--         GPS coordinates at 7 decimal places = 1cm precision.
--         NUMERIC is exact decimal arithmetic.
-- ============================================================

-- ─── ROOMS ───────────────────────────────────────────────────
CREATE TABLE rooms (
    room_id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(20)  NOT NULL,   -- A201, SH-01, LAB-CS-3
    display_name    VARCHAR(100) NULL,       -- "Seminar Hall Block A"
    room_type       VARCHAR(20)  NOT NULL,
    capacity        INTEGER     NOT NULL,
    -- bench grid (NULL for non-exam rooms - DECISION [D7])
    bench_rows      INTEGER     NULL,
    bench_cols      INTEGER     NULL,
    floor_number    INTEGER     NOT NULL DEFAULT 0,
    block           VARCHAR(10)  NOT NULL,   -- A, B, C, D, Admin, Library
    building        VARCHAR(50)  NULL,       -- Main Block, PG Block
    -- GPS for campus map (NUMERIC not FLOAT - DECISION [D12])
    latitude        NUMERIC(10,7) NULL,
    longitude       NUMERIC(10,7) NULL,
    directions_text TEXT        NULL,        -- "From main gate, Block A, 2nd floor..."
    dept_owner_id   UUID        NULL REFERENCES departments(department_id)
                                    ON DELETE SET NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_room_name       UNIQUE (name),
    CONSTRAINT chk_room_type      CHECK (room_type IN (
        'CLASSROOM','LAB','SEMINAR_HALL','EXAM_HALL','CONFERENCE_ROOM'
    )),
    CONSTRAINT chk_capacity       CHECK (capacity > 0 AND capacity <= 500),
    CONSTRAINT chk_bench_positive CHECK (
        (bench_rows IS NULL AND bench_cols IS NULL)
        OR (bench_rows > 0 AND bench_cols > 0)
    ),
    CONSTRAINT chk_floor          CHECK (floor_number >= 0 AND floor_number <= 10)
);

COMMENT ON TABLE  rooms            IS 'All bookable spaces on RVCE campus';
COMMENT ON COLUMN rooms.bench_rows IS 'NULL for non-exam rooms; exam halls must set both rows and cols';
COMMENT ON COLUMN rooms.latitude   IS 'NUMERIC(10,7) = ~1cm GPS precision. Not FLOAT (rounding errors).';

CREATE TRIGGER trg_rooms_updated_at
    BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ─── TIMETABLE_VERSIONS ──────────────────────────────────────
-- DECISION [D8]: versioning without deleting old data
CREATE TABLE timetable_versions (
    version_id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year   VARCHAR(10)  NOT NULL,   -- 2025-26
    semester        INTEGER     NOT NULL,    -- 1-8
    label           VARCHAR(100) NULL,       -- "Post-mid revision"
    status          VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
    created_by      UUID        NOT NULL REFERENCES users(user_id),
    activated_at    TIMESTAMPTZ NULL,        -- when status->ACTIVE
    archived_at     TIMESTAMPTZ NULL,        -- when status->ARCHIVED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tt_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_semester   CHECK (semester BETWEEN 1 AND 8),
    -- DECISION: only one ACTIVE version per semester per year
    -- Prevents ambiguity when querying "what is today's schedule?"
    CONSTRAINT uq_active_semester UNIQUE (academic_year, semester, status)
        DEFERRABLE INITIALLY DEFERRED   -- allow transition: ACTIVE->ARCHIVED and DRAFT->ACTIVE in one txn
);

-- ─── TIMETABLE_SLOTS ─────────────────────────────────────────
-- Core scheduling table. One row = one recurring weekly slot.
-- DECISION: stores day_of_week + period_number (recurring pattern)
-- not specific dates. day_overrides handles specific-date exceptions.
-- This avoids inserting 180 rows (one per semester day) per slot.
CREATE TABLE timetable_slots (
    slot_id         BIGSERIAL   PRIMARY KEY,
    version_id      UUID        NOT NULL REFERENCES timetable_versions(version_id),
    room_id         UUID        NOT NULL REFERENCES rooms(room_id),
    teacher_id      UUID        NOT NULL REFERENCES users(user_id),
    department      VARCHAR(100) NOT NULL,   -- Computer Science, Information Science
    subject_code    VARCHAR(20)  NOT NULL,   -- 21CS51
    subject_name    VARCHAR(100) NOT NULL,   -- Design & Analysis of Algorithms
    section         VARCHAR(5)   NOT NULL,   -- A, B, C, 1, 2
    semester        INTEGER     NOT NULL,
    day_of_week     INTEGER     NOT NULL,    -- 1=Monday, 5=Friday
    period_number   INTEGER     NOT NULL,    -- 1=8:00am, 8=5:00pm
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    -- DECISION [D9]: optimistic locking column
    -- JPA @Version maps to this. Never set manually.
    row_version     INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_slot_day    CHECK (day_of_week BETWEEN 1 AND 5),
    CONSTRAINT chk_slot_period CHECK (period_number BETWEEN 1 AND 8),
    CONSTRAINT chk_slot_time   CHECK (end_time > start_time),
    -- DECISION: room cannot be double-booked in same version
    -- DB enforces this even if application validation is bypassed
    CONSTRAINT uq_slot_room_time UNIQUE (version_id, room_id, day_of_week, period_number),
    -- DECISION: teacher cannot be in two rooms simultaneously
    CONSTRAINT uq_slot_teacher_time UNIQUE (version_id, teacher_id, day_of_week, period_number)
);

COMMENT ON COLUMN timetable_slots.row_version IS '@Version field for JPA optimistic locking. Do not update manually.';
COMMENT ON COLUMN timetable_slots.day_of_week IS '1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday';

-- ─── DAY_OVERRIDES ───────────────────────────────────────────
-- DECISION [D10]: separate from timetable_slots
-- Records: class cancelled today / room claimed by another teacher
-- DECISION: override_id uses BIGSERIAL for auto-increment IDs (aligns with JPA entity)
CREATE TABLE day_overrides (
    override_id         BIGSERIAL   PRIMARY KEY,
    slot_id             BIGINT      NOT NULL REFERENCES timetable_slots(slot_id)
                                        ON DELETE CASCADE,
    override_date       DATE        NOT NULL,
    status              VARCHAR(15)  NOT NULL,
    -- acting_teacher_id: who claimed this slot (NULL if just cancelled)
    acting_teacher_id   UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL,
    reason              TEXT        NULL,
    source              VARCHAR(10)  NOT NULL DEFAULT 'TEACHER',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- DECISION: one override per slot per date
    -- Prevents: teacher cancels, then tries to cancel again same day
    CONSTRAINT uq_override_slot_date UNIQUE (slot_id, override_date),
    CONSTRAINT chk_override_status CHECK (
        status IN ('CANCELLED','CLAIMED','EXTRA_CLASS')
    ),
    CONSTRAINT chk_override_source CHECK (
        source IN ('TEACHER','ADMIN','SYSTEM')
    )
);

COMMENT ON COLUMN day_overrides.acting_teacher_id IS 'NULL when status=CANCELLED. Set when another teacher claims the freed slot.';

-- ─── BLACKOUT_DATES ──────────────────────────────────────────
-- DECISION: separate from day_overrides because blackouts are
-- GLOBAL policy (no classes on Diwali) not slot-specific.
-- dept_id NULL = college-wide blackout. Set = dept-only holiday.
CREATE TABLE blackout_dates (
    blackout_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    blackout_date   DATE        NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    scope           VARCHAR(10)  NOT NULL DEFAULT 'GLOBAL',
    dept_id         UUID        NULL REFERENCES departments(department_id),
    created_by      UUID        NOT NULL REFERENCES users(user_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_blackout_date_dept UNIQUE (blackout_date, dept_id),
    CONSTRAINT chk_blackout_scope CHECK (scope IN ('GLOBAL','DEPT'))
);

-- ─── OCCUPANCY_RECORDS ───────────────────────────────────────
-- DECISION [D11]: event log, not current status column
-- Each check is a timestamped record. Enables:
--   "Was room A201 occupied at 10am yesterday?" (forensic query)
-- If we stored only current status on rooms table, history is lost.
CREATE TABLE occupancy_records (
    record_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID        NOT NULL REFERENCES rooms(room_id),
    -- NULL slot_id = ad-hoc check outside scheduled slot
    slot_id         BIGINT      NULL REFERENCES timetable_slots(slot_id)
                                    ON DELETE SET NULL,
    override_id     BIGINT      NULL REFERENCES day_overrides(override_id)
                                    ON DELETE SET NULL,
    check_date      DATE        NOT NULL,
    check_time      TIME        NOT NULL,
    method          VARCHAR(15)  NOT NULL,
    is_occupied     BOOLEAN     NOT NULL,
    person_count    INTEGER     NULL,        -- from YOLO detection; NULL if MANUAL
    ai_confidence   NUMERIC(5,4) NULL,      -- 0.0000 to 1.0000
    image_url       TEXT        NULL,        -- MinIO object path
    verified_by     UUID        NULL REFERENCES users(user_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_occ_method     CHECK (method IN ('MANUAL','IMAGE_AI')),
    CONSTRAINT chk_ai_confidence  CHECK (ai_confidence IS NULL OR
                                         (ai_confidence >= 0 AND ai_confidence <= 1)),
    CONSTRAINT chk_person_count   CHECK (person_count IS NULL OR person_count >= 0),
    -- AI checks must have confidence score
    CONSTRAINT chk_ai_has_confidence CHECK (
        method != 'IMAGE_AI' OR ai_confidence IS NOT NULL
    )
);
