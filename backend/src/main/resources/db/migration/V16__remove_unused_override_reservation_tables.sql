-- ============================================================
-- V16__remove_unused_override_reservation_tables.sql
-- Remove day_overrides and room_reservations tables.
--
-- Context:
--   Override and room reservation features have been removed from the application.
--   This migration safely removes the unused schema and associated foreign keys.
-- ============================================================

-- Drop foreign key from occupancy_records to day_overrides
ALTER TABLE IF EXISTS occupancy_records
    DROP CONSTRAINT IF EXISTS occupancy_records_override_id_fkey;

-- Drop day_overrides table
DROP TABLE IF EXISTS day_overrides CASCADE;

-- Drop room_reservations table
DROP TABLE IF EXISTS room_reservations CASCADE;
