-- link_students.sql
--
-- Load generated student accounts from CSV, insert/update users,
-- assign the STUDENT role, and link exam_students by USN.
--
-- Usage:
-- 1. Copy student_accounts_db_load.csv into the Postgres container at /tmp/student_accounts_db_load.csv
-- 2. Run: psql -U scas -d scas_db -f /tmp/link_students.sql
--
-- NOTE: if `docker compose down -v` was executed, the database volume may have been removed and
-- the data will need to be restored before running this script.

BEGIN;

CREATE TEMP TABLE tmp_students (
  usn character varying(20),
  name character varying(150),
  email character varying(255),
  password_hash character varying(72),
  department_id uuid,
  branch_code character varying(10)
);

COPY tmp_students FROM '/tmp/student_accounts_db_load.csv' WITH (FORMAT csv, HEADER true);

-- Validate the imported CSV before making changes
SELECT 'tmp_row_count' AS info, count(*) FROM tmp_students;
SELECT 'duplicate_usn' AS info, usn, count(*) FROM tmp_students GROUP BY usn HAVING count(*) > 1;
SELECT 'duplicate_email' AS info, email, count(*) FROM tmp_students GROUP BY email HAVING count(*) > 1;

-- Update any existing users that match by email or USN
UPDATE users u
SET
  name = regexp_replace(replace(trim(t.name), E'\n', ' '), E'\s+', ' ', 'g'),
  usn = COALESCE(NULLIF(upper(trim(t.usn)), ''), u.usn),
  department_id = COALESCE(NULLIF(t.department_id::text, '')::uuid, u.department_id),
  password_hash = trim(t.password_hash)
FROM tmp_students t
WHERE lower(trim(u.email)) = lower(trim(t.email))
   OR (u.usn IS NOT NULL AND upper(trim(u.usn)) = upper(trim(t.usn)));

-- Insert new student users that do not already exist by email or USN
INSERT INTO users (name, email, password_hash, usn, department_id)
SELECT
  regexp_replace(replace(trim(name), E'\n', ' '), E'\s+', ' ', 'g'),
  lower(trim(email)),
  trim(password_hash),
  upper(trim(usn)),
  NULLIF(department_id::text, '')::uuid
FROM tmp_students t
WHERE NOT EXISTS (
  SELECT 1 FROM users u
  WHERE lower(trim(u.email)) = lower(trim(t.email))
     OR (u.usn IS NOT NULL AND upper(trim(u.usn)) = upper(trim(t.usn)))
);

-- Grant STUDENT role to all matching users
INSERT INTO user_roles (user_id, role_id, granted_by, granted_at)
SELECT DISTINCT u.user_id, r.role_id, NULL::uuid, now()
FROM users u
JOIN roles r ON r.name = 'STUDENT'
JOIN tmp_students t ON lower(trim(u.email)) = lower(trim(t.email))
                        OR (u.usn IS NOT NULL AND upper(trim(u.usn)) = upper(trim(t.usn)))
ON CONFLICT DO NOTHING;

-- Link exam_students records to the created/updated users by USN
UPDATE exam_students es
SET student_id = u.user_id
FROM users u
WHERE es.student_id IS NULL
  AND upper(trim(es.usn)) = upper(trim(u.usn));

-- Summary counts
SELECT 'created_or_updated_users' AS info, count(*)
FROM users u
WHERE u.usn IN (SELECT upper(trim(usn)) FROM tmp_students);

SELECT 'linked_exam_students' AS info, count(*)
FROM exam_students es
WHERE es.student_id IS NOT NULL
  AND upper(trim(es.usn)) IN (SELECT upper(trim(usn)) FROM tmp_students);

COMMIT;
