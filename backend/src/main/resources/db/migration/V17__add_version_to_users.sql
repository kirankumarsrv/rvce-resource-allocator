-- ============================================================
-- V17__add_version_to_users.sql
-- Add optimistic locking version column to users table
--
-- This prevents StaleObjectStateException during concurrent
-- user updates in DevDataInitializer and other scenarios.
-- ============================================================

ALTER TABLE users
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.version IS 'Optimistic locking version for concurrent update protection';