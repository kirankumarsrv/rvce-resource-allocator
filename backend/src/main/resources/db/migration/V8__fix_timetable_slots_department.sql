-- V8__fix_timetable_slots_department.sql
-- Cleanup migration: remove department_id if it exists
-- (V2 already created department as VARCHAR(100))
-- This handles the case where department_id was created in an intermediate schema

ALTER TABLE timetable_slots DROP CONSTRAINT IF EXISTS timetable_slots_department_id_fkey;
ALTER TABLE timetable_slots DROP COLUMN IF EXISTS department_id;

-- Note: department VARCHAR(100) column is already defined in V2
-- and V6 seed data now uses the correct column name.