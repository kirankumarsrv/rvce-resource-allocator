-- ============================================================
-- V11__manual_seating_dashboard.sql
-- SCAS - Manual seating dashboard schema updates
-- ============================================================

-- Extend exam halls with explicit bench mix counts.
ALTER TABLE exam_halls
    ADD COLUMN IF NOT EXISTS two_seater_count SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS three_seater_count SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_capacity SMALLINT NOT NULL DEFAULT 0;

-- Preserve the hall snapshot if rows already exist.
UPDATE exam_halls
SET total_capacity = COALESCE(NULLIF(total_capacity, 0), assigned_capacity)
WHERE total_capacity = 0;

-- Add seat index within each bench so 2-seater and 3-seater benches can be addressed explicitly.
ALTER TABLE exam_seats
    ADD COLUMN IF NOT EXISTS bench_seat_index SMALLINT NOT NULL DEFAULT 0;

-- Replace the old bench uniqueness rule with the new row/col/seat-index key.
ALTER TABLE exam_seats
    DROP CONSTRAINT IF EXISTS uq_bench_position;

ALTER TABLE exam_seats
    ADD CONSTRAINT uq_exam_seat_position UNIQUE (hall_id, bench_row, bench_col, bench_seat_index);

-- Manual dashboard no longer uses async CSP jobs or JSON constraint rows.
ALTER TABLE exam_seats
    DROP COLUMN IF EXISTS job_id;

DROP TABLE IF EXISTS seating_constraints;
DROP TABLE IF EXISTS seating_jobs;
