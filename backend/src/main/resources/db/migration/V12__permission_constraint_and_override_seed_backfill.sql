-- Backfill migration for environments where V6 was already applied earlier.
-- 1) Expand chk_perm_resource to include timetable_overrides.
ALTER TABLE permissions
    DROP CONSTRAINT IF EXISTS chk_perm_resource;

ALTER TABLE permissions
    ADD CONSTRAINT chk_perm_resource CHECK (
        resource IN ('rooms','timetable','timetable_overrides','exam','reports','notifications','audit','users')
    );

-- 2) Ensure timetable_overrides:read permission exists.
INSERT INTO permissions (permission_id, resource, action, description)
VALUES ('33333333-3333-3333-3333-333333333015', 'timetable_overrides', 'read', 'Read timetable overrides')
ON CONFLICT (resource, action) DO NOTHING;

-- 3) Ensure role mappings exist (TEACHER, TTO, DEPT_COORD).
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222002', '33333333-3333-3333-3333-333333333015'),
    ('22222222-2222-2222-2222-222222222003', '33333333-3333-3333-3333-333333333015'),
    ('22222222-2222-2222-2222-222222222004', '33333333-3333-3333-3333-333333333015')
ON CONFLICT DO NOTHING;
