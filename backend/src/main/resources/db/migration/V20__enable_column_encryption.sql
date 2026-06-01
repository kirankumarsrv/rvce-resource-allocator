-- T-602: Expand columns to support AES-256-GCM encrypted values
-- Encrypted output is base64-encoded and larger than plaintext.

ALTER TABLE users
    ALTER COLUMN email TYPE VARCHAR(500),
    ALTER COLUMN usn TYPE VARCHAR(100);

ALTER TABLE exam_students
    ALTER COLUMN usn TYPE VARCHAR(100);
