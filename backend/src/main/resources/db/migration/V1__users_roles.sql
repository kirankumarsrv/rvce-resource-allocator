-- ============================================================
-- V1__users_roles.sql
-- SCAS - Smart Campus Allocation System
-- Auth, RBAC and user identity layer
--
-- DECISION LOG:
--   [D1] UUID PKs (gen_random_uuid): avoids central sequence
--        bottleneck when we scale to multiple Spring pods.
--        PostgreSQL's gen_random_uuid() is CRYPTOGRAPHICALLY
--        random - no guessable sequential IDs exposed in URLs.
--
--   [D2] departments created BEFORE users because users carry
--        a FK to departments. HOD is set AFTER users exist, so
--        hod_user_id is DEFERRABLE to allow circular inserts
--        in a single transaction.
--
--   [D3] password_hash is VARCHAR(72) not TEXT. BCrypt always
--        produces exactly 60 chars. 72 gives headroom for
--        algorithm prefix. Prevents accidental plain-text
--        storage (a 500-char raw password would truncate,
--        alerting developers immediately).
--
--   [D4] usn is NULLABLE. Non-student users (teachers, admins)
--        have no USN. Making it NOT NULL would force dummy
--        values, polluting data integrity.
--
--   [D5] email uniqueness is enforced on the normalized email value
--        maintained by the application. The Azure-compatible schema
--        uses standard VARCHAR and does not require the citext extension.
--
--   [D6] refresh_tokens stores token_hash (SHA-256 of the raw
--        token), NEVER the raw token. If the DB is breached,
--        the hash is useless - attacker needs the original.
--        Same principle as password hashing.
-- ============================================================

-- PostgreSQL 13+ provides gen_random_uuid() as a built-in function.

-- ─── DEPARTMENTS ─────────────────────────────────────────────
-- Created first because users FK to departments.
-- hod_user_id FK is DEFERRABLE (circular: dept→user→dept).
CREATE TABLE departments (
    department_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(10)  NOT NULL,   -- CSE, ISE, ECE, MECH...
    -- DECISION: hod_user_id set after users exist, so deferred FK
    hod_user_id     UUID        NULL,        -- set after user rows exist
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_dept_code UNIQUE (code),
    CONSTRAINT uq_dept_name UNIQUE (name)
);

COMMENT ON TABLE  departments       IS 'Academic departments at RVCE';
COMMENT ON COLUMN departments.hod_user_id IS 'FK added as DEFERRABLE after users table exists';

-- ─── USERS ───────────────────────────────────────────────────
-- DECISION: email is stored as standard VARCHAR; application-level
-- normalization and email_hash provide lookup and uniqueness support.
-- DECISION: usn nullable - only students have USNs.
-- DECISION: is_active soft-delete instead of hard delete because
--   audit_logs reference actor by email string (denormalised),
--   but exam_seats, timetable_slots still reference user_id FK.
--   Hard delete would cascade and destroy scheduling history.
CREATE TABLE users (
    user_id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(72) NOT NULL,    -- BCrypt output always ~60 chars
    usn             VARCHAR(20)  NULL,       -- 1RV21CS001 format; NULL for staff
    department_id   UUID        NULL REFERENCES departments(department_id)
                                    ON DELETE SET NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    failed_login_count  SMALLINT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ NULL,       -- NULL = not locked
    last_login_at   TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_usn   UNIQUE (usn),
    CONSTRAINT chk_usn_format CHECK (
        usn IS NULL OR usn ~ '^[0-9A-Z]{10,13}$'
    ),
    CONSTRAINT chk_failed_logins CHECK (failed_login_count >= 0)
);

COMMENT ON TABLE  users IS 'All system users: students, teachers, admins';
COMMENT ON COLUMN users.password_hash    IS 'BCrypt hash, strength 12. Never store plain text.';
COMMENT ON COLUMN users.failed_login_count IS 'Incremented per failed login; reset on success. Lock at 5.';
COMMENT ON COLUMN users.locked_until IS 'Account locked until this timestamp. NULL = not locked.';

-- Now add the deferred HOD FK (departments → users circular ref)
-- DEFERRABLE INITIALLY DEFERRED: constraint checked at END of
-- transaction, not per-statement. Allows: INSERT dept (no hod),
-- INSERT user, UPDATE dept SET hod = user_id - all in one txn.
ALTER TABLE departments
    ADD CONSTRAINT fk_dept_hod
    FOREIGN KEY (hod_user_id)
    REFERENCES users(user_id)
    ON DELETE SET NULL
    DEFERRABLE INITIALLY DEFERRED;

-- ─── ROLES ───────────────────────────────────────────────────
-- DECISION: roles as a table, not a ENUM column on users.
-- With ENUM you need ALTER TYPE (full table rewrite in old PG)
-- to add a role. With this table: INSERT INTO roles - zero downtime.
-- Also enables role metadata (description, created_at) for audit.
CREATE TABLE roles (
    role_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL,
    description TEXT        NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_role_name UNIQUE (name),
    -- DECISION: CHECK constraint documents valid values at DB level.
    -- Spring RBAC also enforces this, but DB is the last line of defence.
    CONSTRAINT chk_role_name CHECK (
        name IN ('STUDENT','TEACHER','TTO','DEPT_COORD','ADMIN','EXAM_CONTROLLER','SUPER_ADMIN')
    )
);

COMMENT ON TABLE roles IS 'System roles: STUDENT, TEACHER, TTO, DEPT_COORD, ADMIN';

-- ─── PERMISSIONS ─────────────────────────────────────────────
-- DECISION: fine-grained permission model (resource + action pairs)
-- not just role names. This allows future: "teacher can READ rooms
-- but not WRITE timetable" without schema change.
CREATE TABLE permissions (
    permission_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    resource        VARCHAR(50)  NOT NULL,  -- rooms, timetable, exam, reports
    action          VARCHAR(20)  NOT NULL,  -- read, write, delete, publish
    description     TEXT        NULL,

    CONSTRAINT uq_perm UNIQUE (resource, action),
    CONSTRAINT chk_perm_resource CHECK (
        resource IN ('rooms','timetable','exam','reports','notifications','audit','users')
    ),
    CONSTRAINT chk_perm_action CHECK (
        action IN ('read','write','delete','publish','verify','generate')
    )
);

-- ─── USER_ROLES (junction) ────────────────────────────────────
-- DECISION: composite PK (user_id + role_id) prevents duplicate
-- role assignments. granted_by creates accountability chain.
-- expires_at supports time-limited roles (e.g., temp exam coordinator).
CREATE TABLE user_roles (
    user_id         UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id         UUID        NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    granted_by      UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NULL,   -- NULL = permanent

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id)
);

-- ─── ROLE_PERMISSIONS (junction) ─────────────────────────────
CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id)
);

-- ─── REFRESH_TOKENS ──────────────────────────────────────────
-- DECISION: token_hash is SHA-256 of the raw token.
-- Raw token is returned to client ONCE and never stored.
-- DECISION: ip_address + user_agent stored for anomaly detection
-- (new device from different country = suspicious, force re-auth).
CREATE TABLE refresh_tokens (
    token_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL,   -- hex of SHA-256
    ip_address      INET        NULL,
    user_agent      TEXT        NULL,
    is_revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_token_hash UNIQUE (token_hash)
);

-- Trigger: auto-update updated_at on users
-- DECISION: trigger over application-layer update because if
-- any DB tool (psql CLI, migration script) updates the row
-- directly, updated_at still gets set correctly.
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_departments_updated_at
    BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
