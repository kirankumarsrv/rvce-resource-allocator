-- V9__add_is_active_to_timetable_slots.sql
-- Add is_active column to timetable_slots table to match JPA entity

ALTER TABLE timetable_slots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN timetable_slots.is_active IS 'Soft delete flag for timetable slots';