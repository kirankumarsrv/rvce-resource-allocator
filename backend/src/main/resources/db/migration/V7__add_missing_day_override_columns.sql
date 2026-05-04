-- ============================================================
-- V7__add_missing_day_override_columns.sql
-- Add missing columns to day_overrides table for schema alignment with JPA entity.
--
-- Context:
--   Original V2 schema has day_overrides with UUID primary key (override_id).
--   But DayOverride JPA entity expects:
--     - id BIGINT (auto-increment)
--     - created_by UUID (not null)
--
-- This migration safely adds missing columns for both:
--   - Fresh installations (after V2)
--   - Upgrades from legacy schema
-- ============================================================

-- Add created_by column if missing
ALTER TABLE IF EXISTS day_overrides
    ADD COLUMN IF NOT EXISTS created_by UUID;

-- Add id BIGINT column if missing (for future PK migration)
-- This allows the entity to work while we keep override_id as the current PK
ALTER TABLE IF EXISTS day_overrides
    ADD COLUMN IF NOT EXISTS id BIGSERIAL UNIQUE;

-- Backfill missing created_by values with system UUID (for upgrades)
UPDATE day_overrides
SET created_by = '00000000-0000-0000-0000-000000000000'
WHERE created_by IS NULL;

-- Add NOT NULL constraint to created_by
ALTER TABLE IF EXISTS day_overrides
    ALTER COLUMN created_by SET NOT NULL;

-- Index for created_by queries
CREATE INDEX IF NOT EXISTS idx_do_created_by ON day_overrides(created_by);

-- Verify schema alignment (comment for verification)
-- Expected columns: override_id, slot_id, override_date, status, acting_teacher_id, 
--                  reason, source, created_at, created_by, id
