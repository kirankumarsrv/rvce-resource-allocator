-- Ensure permissions.resource accepts timetable_overrides before V6 seeds run.
ALTER TABLE permissions
    DROP CONSTRAINT IF EXISTS chk_perm_resource;

ALTER TABLE permissions
    ADD CONSTRAINT chk_perm_resource CHECK (
        resource IN ('rooms','timetable','timetable_overrides','exam','reports','notifications','audit','users')
    );
