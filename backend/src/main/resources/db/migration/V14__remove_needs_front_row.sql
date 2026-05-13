-- ============================================================
-- V14__remove_needs_front_row.sql
-- SCAS - Remove needs_front_row field from exam_students
--
-- DECISION: The needs_front_row field was used for legacy
-- accessibility constraints, but the new bulk allocation engine
-- doesn't use this field. Removing to simplify the schema and
-- eliminate the "FAILED TO CREATE EXAM SESSION" errors.
-- ============================================================

-- Drop the needs_front_row column from exam_students table
ALTER TABLE exam_students DROP COLUMN needs_front_row;

-- Update the comment on exam_students table
COMMENT ON TABLE exam_students IS 'Student enrollment rows for an exam session';
