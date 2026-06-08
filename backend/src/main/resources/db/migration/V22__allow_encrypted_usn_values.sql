-- V22__allow_encrypted_usn_values.sql
-- Relax the users.usn constraint so encrypted USN values can be stored,
-- while still clearing legacy values that are neither valid plaintext nor valid encrypted payloads.

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_usn_format;

ALTER TABLE users
    ADD CONSTRAINT chk_usn_format CHECK (
        usn IS NULL OR
        usn ~ '^[0-9A-Z]{10,13}$' OR
        usn ~ '^[A-Za-z0-9+/=]{24,100}$'
    );

UPDATE users
SET usn = NULL
WHERE usn IS NOT NULL
  AND usn !~ '^[0-9A-Z]{10,13}$'
  AND usn !~ '^[A-Za-z0-9+/=]{24,100}$';
