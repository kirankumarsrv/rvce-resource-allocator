-- ============================================================
-- V4__notifications_audit.sql
-- SCAS - Notifications, FCM Tokens, Reports, Audit Logs
--
-- DECISION LOG:
--   [D19] audit_logs has NO FOREIGN KEYS.
--         FKs on an audit table would prevent deleting a user
--         (e.g., after legal retention period or GDPR request)
--         because audit records reference them. With NO FKs,
--         audit records survive independently forever.
--         actor_email and actor_role are DENORMALISED here so
--         the audit trail is human-readable even after user deletion.
--
--   [D20] notifications.payload is JSONB.
--         The notification body is a string ("Your seat is ready")
--         but the action data needs structure for deep-linking:
--           {"exam_id": "...", "room": "A201", "floor": 2, "bench": "B-14"}
--         Without JSONB, frontend can't render a "Navigate" button.
--         Pre-defining every notification type's columns would
--         require schema changes for each new notification type.
--
--   [D21] fcm_device_tokens is a SEPARATE TABLE from users.
--         One user can have multiple devices (phone + laptop + tablet).
--         A single fcm_token column on users supports only the last
--         registered device. This table enables fan-out:
--           SELECT * FROM fcm_device_tokens
--           WHERE user_id = ? AND is_active = TRUE
--         -> send to ALL their devices simultaneously.
--
--   [D22] notification_batches tracks bulk sends.
--         When exam seats are published, 500+ students are notified
--         simultaneously. Without batch tracking, there's no way to
--         know: how many were delivered? how many failed? which failed?
--         This enables retry logic: re-send only to failed recipients.
--
--   [D23] reports table stores MinIO presigned URL + expiry.
--         PDFs are generated on-demand and cached in MinIO for 1 hour.
--         If the coordinator re-requests within 1 hour: return cached URL.
--         After expiry: generate fresh PDF. Avoids re-running expensive
--         iText PDF generation on every request.
--
--   [D24] audit_logs.created_at has no DEFAULT.
--         It is set by the application at the moment of the event,
--         not at DB insert time (which could be slightly later due to
--         async audit dispatch). Precise event timing matters for
--         forensic reconstruction of sequences.
--         CORRECTION: DEFAULT NOW() is fine and safer.
-- ============================================================

-- ─── FCM_DEVICE_TOKENS ───────────────────────────────────────
-- DECISION [D21]: separate table for multi-device fan-out
CREATE TABLE fcm_device_tokens (
    token_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(user_id)
                                    ON DELETE CASCADE,
    fcm_token       TEXT        NOT NULL,    -- Firebase device token (varies in length)
    platform        VARCHAR(10)  NOT NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_seen_at    TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_fcm_token  UNIQUE (fcm_token),
    CONSTRAINT chk_platform  CHECK (platform IN ('WEB','ANDROID','IOS'))
);

COMMENT ON TABLE fcm_device_tokens IS 'One user may register multiple FCM tokens (phone + laptop). Fan-out sends to all active tokens.';

-- ─── NOTIFICATION_BATCHES ────────────────────────────────────
-- DECISION [D22]: tracks bulk notification sends
CREATE TABLE notification_batches (
    batch_id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    trigger_event       VARCHAR(30)  NOT NULL,
    triggered_by        UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL,
    ref_entity_type     VARCHAR(20)  NULL,   -- EXAM, SLOT, TIMETABLE
    ref_entity_id       UUID        NULL,
    total_recipients    INTEGER     NOT NULL DEFAULT 0,
    sent_count          INTEGER     NOT NULL DEFAULT 0,
    failed_count        INTEGER     NOT NULL DEFAULT 0,
    status              VARCHAR(15)  NOT NULL DEFAULT 'QUEUED',
    started_at          TIMESTAMPTZ NULL,
    completed_at        TIMESTAMPTZ NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_batch_event CHECK (trigger_event IN (
        'EXAM_PUBLISHED','SLOT_CANCELLED','ROOM_FREED',
        'TIMETABLE_UPDATED','SYSTEM_ALERT'
    )),
    CONSTRAINT chk_batch_status CHECK (
        status IN ('QUEUED','PROCESSING','DONE','PARTIAL','FAILED')
    ),
    CONSTRAINT chk_counts_positive CHECK (
        total_recipients >= 0 AND sent_count >= 0 AND failed_count >= 0
    )
);

-- ─── NOTIFICATIONS ───────────────────────────────────────────
-- DECISION [D20]: JSONB payload for structured deep-link data
CREATE TABLE notifications (
    notification_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(user_id)
                                    ON DELETE CASCADE,
    batch_id        UUID        NULL REFERENCES notification_batches(batch_id)
                                    ON DELETE SET NULL,
    notif_type      VARCHAR(30)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT        NOT NULL,
    -- JSONB payload for frontend deep-linking (DECISION [D20])
    payload         JSONB       NOT NULL DEFAULT '{}',
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    delivery_status VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    fcm_message_id  VARCHAR(200) NULL,
    ref_entity_id   UUID        NULL,        -- exam_id or slot_id
    ref_entity_type VARCHAR(20)  NULL,
    read_at         TIMESTAMPTZ NULL,
    sent_at         TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_notif_type CHECK (notif_type IN (
        'CLASS_CANCELLED','SEAT_PUBLISHED','ROOM_FREED',
        'TIMETABLE_UPDATED','EXAM_REMINDER','SYSTEM'
    )),
    CONSTRAINT chk_delivery_status CHECK (
        delivery_status IN ('PENDING','SENT','FAILED','READ')
    ),
    -- read_at only set when is_read = TRUE
    CONSTRAINT chk_read_consistency CHECK (
        (is_read = TRUE AND read_at IS NOT NULL)
        OR is_read = FALSE
    )
);

-- ─── REPORTS ─────────────────────────────────────────────────
-- DECISION [D23]: cache generated PDFs in MinIO with presigned URL
CREATE TABLE reports (
    report_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type     VARCHAR(25)  NOT NULL,
    generated_by    UUID        NULL REFERENCES users(user_id) ON DELETE SET NULL,
    -- Parameters used to generate (for cache key matching)
    parameters      JSONB       NOT NULL DEFAULT '{}',
    file_url        TEXT        NULL,       -- MinIO presigned path (set after generation)
    file_format     VARCHAR(10)  NOT NULL DEFAULT 'PDF',
    file_size_bytes BIGINT      NULL,
    expires_at      TIMESTAMPTZ NULL,       -- presigned URL expiry
    generation_ms   INTEGER     NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_report_type   CHECK (report_type IN (
        'HALL_SEATING_PLAN','TIMETABLE','ROOM_AVAILABILITY',
        'OCCUPANCY_SUMMARY','EXAM_ABSENTEE'
    )),
    CONSTRAINT chk_report_format CHECK (file_format IN ('PDF','EXCEL','CSV'))
);

-- ─── AUDIT_LOGS ──────────────────────────────────────────────
-- DECISION [D19]: NO FOREIGN KEYS. Immutable event log.
-- Denormalised actor fields survive user deletion.
--
-- DECISION: No UPDATE or DELETE should EVER run on this table.
-- Enforce with a trigger that raises an exception on any attempt.
CREATE TABLE audit_logs (
    log_id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NO FK - immutable (DECISION [D19])
    actor_id        UUID        NOT NULL,       -- user_id at time of action
    actor_email     VARCHAR(255) NOT NULL,      -- denormalised
    actor_role      VARCHAR(20)  NOT NULL,      -- denormalised
    action          VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(30)  NOT NULL,
    entity_id       UUID        NULL,           -- NULL for login events
    old_value       JSONB       NULL,           -- state before change
    new_value       JSONB       NULL,           -- state after change
    ip_address      INET        NULL,
    user_agent      TEXT        NULL,
    success         BOOLEAN     NOT NULL DEFAULT TRUE,
    error_message   TEXT        NULL,           -- set when success=FALSE
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_audit_action CHECK (action IN (
        'LOGIN','LOGOUT','REGISTER',
        'UPLOAD_TIMETABLE','ACTIVATE_TIMETABLE',
        'CANCEL_SLOT','CLAIM_SLOT',
        'GENERATE_SEATING','PUBLISH_EXAM','COMPLETE_EXAM',
        'VERIFY_OCCUPANCY',
        'SEND_NOTIFICATION',
        'CREATE_USER','UPDATE_USER','DEACTIVATE_USER',
        'CREATE_ROOM','UPDATE_ROOM','DEACTIVATE_ROOM'
    )),
    CONSTRAINT chk_audit_entity CHECK (entity_type IN (
        'USER','ROOM','TIMETABLE_SLOT','DAY_OVERRIDE',
        'EXAM_SESSION','EXAM_SEAT','NOTIFICATION','SYSTEM'
    ))
);

COMMENT ON TABLE  audit_logs IS 'Immutable event log. NO updates or deletes. NO foreign keys (survives entity deletion).';
COMMENT ON COLUMN audit_logs.actor_email IS 'Denormalised: human-readable even after user is deleted.';

-- DECISION: Prevent any modification to audit_logs after insert.
-- This is a security control - even a compromised DB user cannot
-- cover their tracks by updating audit records.
CREATE OR REPLACE FUNCTION fn_protect_audit_log()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is immutable: UPDATE and DELETE are not permitted.';
END;
$$;

CREATE TRIGGER trg_protect_audit_log
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION fn_protect_audit_log();
