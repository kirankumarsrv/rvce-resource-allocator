-- V21__fix_invalid_usn_format.sql
-- Fix invalid USN format in existing users
-- Constraint requires: usn IS NULL OR usn ~ '^[0-9A-Z]{10,13}$'

-- Set USN to NULL for all entries where it doesn't match the constraint
UPDATE users
SET usn = NULL
WHERE usn IS NOT NULL 
  AND usn !~ '^[0-9A-Z]{10,13}$';
