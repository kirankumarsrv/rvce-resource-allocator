-- =============================================================================
-- V28__normalize_exam_student_usn_values.sql
--
-- Repairs exam_students rows that were written while the app was using a
-- temporary AES key. Those rows can no longer be decrypted after restart.
--
-- We restore the USN from the linked users table for every row that already
-- has a student_id. This is deterministic because users.usn was seeded as
-- plain text in the database.
-- =============================================================================

BEGIN;

UPDATE exam_students es
SET usn = u.usn
FROM users u
WHERE es.student_id IS NOT NULL
  AND es.student_id = u.user_id
  AND es.usn IS DISTINCT FROM u.usn;

COMMIT;