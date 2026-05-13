-- ============================================================
-- V19__update_role_constraint.sql
-- SCAS - Update role constraint to include EXAM_CONTROLLER and SUPER_ADMIN
-- ============================================================

ALTER TABLE roles
    DROP CONSTRAINT IF EXISTS chk_role_name;

ALTER TABLE roles
    ADD CONSTRAINT chk_role_name CHECK (
        name IN ('STUDENT','TEACHER','TTO','DEPT_COORD','ADMIN','EXAM_CONTROLLER','SUPER_ADMIN')
    );
