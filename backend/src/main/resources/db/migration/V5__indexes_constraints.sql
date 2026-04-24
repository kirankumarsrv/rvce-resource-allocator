-- ============================================================
-- V5__indexes_constraints.sql
-- SCAS - Performance Indexes, Check Constraints
--
-- DECISION LOG - INDEX STRATEGY:
--
--   INDEX RULE: Every index speeds up reads but SLOWS writes
--   (PostgreSQL must update the index on every INSERT/UPDATE/DELETE).
--   Each index below has a documented query justification.
--   We do NOT index every column - only those in WHERE, JOIN ON,
--   and ORDER BY clauses of the TOP 10 hottest queries.
--
--   [I1] idx_slots_room_day_period (COMPOSITE, B-TREE)
--        Query: "Is room A201 free on Wednesday period 3?"
--        SELECT * FROM timetable_slots
--        WHERE room_id = ? AND day_of_week = ? AND period_number = ?
--        Without index: full seq scan of ~50,000 rows (all slots all semesters).
--        With index: binary search -> ~log2(50000) = 16 comparisons.
--        Column order matters: room_id first (highest cardinality = most selective).
--        Adding version_id: lets WHERE version_id = ? use the same index.
--
--   [I2] idx_exam_seats_student_exam (COMPOSITE, B-TREE)
--        Query: "Show my exam seat" - runs on EVERY student login.
--        Peak load: all students check simultaneously after exam publish.
--        student_id first because we always filter by logged-in user.
--
--   [I3] idx_rooms_name_gin (GIN, full-text search)
--        GIN = Generalised Inverted Index. Tokenises text into
--        words -> maps words back to rows. Supports @@ operator.
--        B-TREE on VARCHAR only helps with LIKE 'prefix%' queries.
--        GIN supports: WHERE to_tsvector('english', name) @@ plainto_tsquery('seminar hall B')
--        DECISION: 'english' dictionary applies stemming (rooms->room, halls->hall).
--
--   [I4] idx_day_overrides_slot_date (COMPOSITE, B-TREE)
--        The real-time dashboard joins timetable_slots + day_overrides
--        to check "is this slot cancelled today?"
--        Without index: nested loop join degrades to O(n*m).
--
--   [I5] idx_notifs_user_unread (PARTIAL, B-TREE)
--        PARTIAL index: WHERE is_read = FALSE.
--        Only indexes unread rows - a tiny fraction of the table.
--        Read notifications accumulate in millions but don't bloat index.
--        created_at DESC: pre-sorted for "newest first" display.
--
--   [I6] idx_constraints_params_gin (GIN, JSONB)
--        CSP engine queries: "find EXCLUSION_ZONE constraints with
--        positions containing [2,3]" using @> (containment).
--        JSONB GIN is the only index type that supports @>.
--
--   [I7] idx_occupancy_room_date (COMPOSITE, B-TREE)
--        "Show all occupancy checks for room X on date Y"
--        Used in the occupancy history dashboard.
--
--   [I8] idx_audit_actor_date (COMPOSITE, B-TREE)
--        "Show all actions by user X in the last 7 days"
--        actor_id first, created_at for range filter.
--
--   [I9] idx_slots_teacher (B-TREE)
--        Teacher substitution query: "Find all slots where
--        teacher_id = replaced_teacher"
--        Runs at every substitution event.
--
--   [I10] idx_exam_students_exam (B-TREE)
--         "List all students in exam X"
--         Runs at CSP start and for seating plan display.
-- ============================================================

-- ── I1: Room availability (hottest query) ────────────────────
-- DECISION: Include version_id as 4th column so queries filtering
-- by active version can use this same index without a separate one.
CREATE INDEX idx_slots_room_day_period
    ON timetable_slots (room_id, day_of_week, period_number, version_id);

-- ── I2: Student exam seat lookup ─────────────────────────────
CREATE INDEX idx_exam_seats_student_exam
    ON exam_seats (student_id, exam_id);

-- Also: coordinator view "all seats for this exam"
CREATE INDEX idx_exam_seats_exam
    ON exam_seats (exam_id, hall_id, bench_row, bench_col);

-- ── I3: Full-text room search ────────────────────────────────
-- DECISION: use 'simple' dictionary not 'english' because room names
-- like "A201" and "SH-01" are codes, not English words.
-- 'simple' lowercases only; 'english' applies stemming (may mangle codes).
CREATE INDEX idx_rooms_name_gin
    ON rooms USING GIN (to_tsvector('simple', name || ' ' || COALESCE(display_name, '')));

-- ── I4: Day overrides lookup ─────────────────────────────────
CREATE INDEX idx_day_overrides_slot_date
    ON day_overrides (slot_id, override_date);

-- Also: "what overrides happened today?" (admin dashboard)
CREATE INDEX idx_day_overrides_date
    ON day_overrides (override_date DESC);

-- ── I5: Unread notifications (partial index) ─────────────────
-- DECISION: Partial index (WHERE is_read = FALSE) = only indexes
-- the minority of rows that are unread. As notifications are read,
-- they fall out of the index automatically. Stays small forever.
CREATE INDEX idx_notifs_user_unread
    ON notifications (user_id, created_at DESC)
    WHERE is_read = FALSE;

-- ── I6: JSONB seating constraints ────────────────────────────
CREATE INDEX idx_constraints_params_gin
    ON seating_constraints USING GIN (parameters);

-- ── I7: Occupancy history ────────────────────────────────────
CREATE INDEX idx_occupancy_room_date
    ON occupancy_records (room_id, check_date DESC);

-- ── I8: Audit log by actor ───────────────────────────────────
CREATE INDEX idx_audit_actor_date
    ON audit_logs (actor_id, created_at DESC);

-- Also: compliance query "all actions of type X in date range"
CREATE INDEX idx_audit_action_date
    ON audit_logs (action, created_at DESC);

-- ── I9: Teacher substitution query ───────────────────────────
CREATE INDEX idx_slots_teacher
    ON timetable_slots (teacher_id, version_id);

-- ── I10: Exam students list ───────────────────────────────────
CREATE INDEX idx_exam_students_exam
    ON exam_students (exam_id);

-- Branch filter for CSP: "students of branch CSE in this exam"
CREATE INDEX idx_exam_students_exam_branch
    ON exam_students (exam_id, branch_code);

-- ── Additional useful indexes ─────────────────────────────────

-- Active refresh tokens lookup (on every authenticated request)
CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens (user_id, is_revoked, expires_at)
    WHERE is_revoked = FALSE;

-- Seating job status polling
CREATE INDEX idx_seating_jobs_exam_status
    ON seating_jobs (exam_id, status, created_at DESC);

-- Notification batches by exam
CREATE INDEX idx_notif_batches_entity
    ON notification_batches (ref_entity_id, ref_entity_type)
    WHERE ref_entity_id IS NOT NULL;

-- FCM token lookup for fan-out
CREATE INDEX idx_fcm_tokens_user_active
    ON fcm_device_tokens (user_id, is_active)
    WHERE is_active = TRUE;

-- Blackout dates range queries
CREATE INDEX idx_blackout_dates_date
    ON blackout_dates (blackout_date);

-- Timetable versions by semester
CREATE INDEX idx_tt_versions_semester
    ON timetable_versions (academic_year, semester, status);

-- Report cache lookup (same parameters = return cached URL)
-- NOTE: partial index predicates cannot use non-IMMUTABLE functions
-- like NOW(), so keep predicate static and filter expires_at at query time.
CREATE INDEX idx_reports_type_params
    ON reports USING GIN (parameters)
    WHERE file_url IS NOT NULL;

-- ── ADDITIONAL CHECK CONSTRAINTS ─────────────────────────────
-- DECISION: Add cross-table consistency checks as DB constraints,
-- not just application-level validation. DB is the last line of defence.

-- Exam seats: bench_number format must be "LETTER-NUMBER" (A-12)
ALTER TABLE exam_seats
    ADD CONSTRAINT chk_bench_number_format
    CHECK (bench_number ~ '^[A-Z]-[0-9]{1,3}$');

-- Timetable slots: start/end times must be within college hours
ALTER TABLE timetable_slots
    ADD CONSTRAINT chk_slot_college_hours
    CHECK (start_time >= '08:00' AND end_time <= '18:00');

-- Occupancy AI confidence must be 4 decimal places max
ALTER TABLE occupancy_records
    ADD CONSTRAINT chk_ai_confidence_precision
    CHECK (ai_confidence IS NULL OR ai_confidence = ROUND(ai_confidence, 4));

-- report presigned URLs must be https (security)
ALTER TABLE reports
    ADD CONSTRAINT chk_file_url_https
    CHECK (file_url IS NULL OR file_url LIKE 'https://%');
