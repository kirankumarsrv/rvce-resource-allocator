-- Add version column for optimistic locking to exam_seats table
ALTER TABLE exam_seats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;