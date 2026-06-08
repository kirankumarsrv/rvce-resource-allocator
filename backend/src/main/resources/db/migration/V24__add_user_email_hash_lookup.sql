-- ============================================================
-- V24__add_user_email_hash_lookup.sql
-- SCAS - Add searchable email hash lookup for encrypted user emails
-- ============================================================

ALTER TABLE users
    ADD COLUMN email_hash VARCHAR(64);

-- Backfill plaintext email rows for existing seed data.
UPDATE users
SET email_hash = encode(digest(lower(email), 'sha256'), 'hex')
WHERE email_hash IS NULL
  AND email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$';

ALTER TABLE users
    ADD CONSTRAINT uq_users_email_hash UNIQUE (email_hash);

CREATE INDEX idx_users_email_hash ON users(email_hash);
