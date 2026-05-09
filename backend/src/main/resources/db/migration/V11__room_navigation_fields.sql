-- ============================================================
-- V11__room_navigation_fields.sql
-- Epic 2 - Campus Navigation System
-- Extends rooms table with navigation-specific fields
--
-- DECISION LOG:
--   [D-01] landmark_description (TEXT NULL) stores natural language
--          descriptions for human identification (e.g., "Blue building
--          next to basketball court"). Helps students without GPS.
--
--   [D-02] floor_plan_s3_key (VARCHAR(255) NULL) stores the S3 object
--          key for floor plan images. NOT the full URL - only the key.
--          The backend generates signed URLs on-demand with 1-hour TTL.
--          Storing the binary image as bytea would bloat the DB and
--          prevent CDN caching. NULL if no floor plan is available.
--
--   [D-03] room_directions table stores pre-seeded step-by-step walking
--          instructions from known start points (MAIN_GATE, LIBRARY, CANTEEN)
--          to each room. Indexed by (room_id, from_location_tag) for
--          fast lookup. step_order ensures deterministic ordering.
-- ============================================================

-- ─── EXTEND ROOMS TABLE ───────────────────────────────────────
ALTER TABLE rooms
    ADD COLUMN landmark_description TEXT NULL,
    ADD COLUMN floor_plan_s3_key VARCHAR(255) NULL;

COMMENT ON COLUMN rooms.landmark_description IS 'Natural language description for human identification (e.g., "Blue building next to basketball court")';
COMMENT ON COLUMN rooms.floor_plan_s3_key IS 'S3 object key for floor plan image (e.g., "floor-plans/{roomId}/{timestamp}.png"). NULL if no floor plan available. Backend generates signed URLs with 1-hour TTL.';

-- ─── ROOM_DIRECTIONS TABLE ───────────────────────────────────
-- DECISION [D-03]: Pre-seeded walking instructions from known start points.
-- No live API calls required; works offline; zero cost.
-- from_location_tag identifies the start point (e.g., 'MAIN_GATE', 'LIBRARY').
-- step_order ensures deterministic ordering for consistent user experience.
CREATE TABLE room_directions (
    direction_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id             UUID        NOT NULL REFERENCES rooms(room_id)
                                        ON DELETE CASCADE,
    from_location_tag   VARCHAR(50) NOT NULL,   -- MAIN_GATE, LIBRARY, CANTEEN
    step_order          SMALLINT    NOT NULL,   -- 1, 2, 3, ... (ordering)
    instruction         TEXT        NOT NULL,   -- "Walk 50m straight towards the library"
    distance_meters     SMALLINT    NOT NULL,   -- Cumulative distance
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_distance_positive CHECK (distance_meters >= 0),
    CONSTRAINT chk_step_order_positive CHECK (step_order > 0),
    -- One set of directions per (room_id, from_location_tag)
    -- Multiple step_orders per set, but never duplicate (room_id, from_location_tag, step_order)
    CONSTRAINT uq_room_directions UNIQUE (room_id, from_location_tag, step_order)
);

COMMENT ON TABLE room_directions IS 'Pre-seeded walking directions from known start points to each room. No live API calls.';
COMMENT ON COLUMN room_directions.from_location_tag IS 'Start point: MAIN_GATE, LIBRARY, CANTEEN, etc.';
COMMENT ON COLUMN room_directions.step_order IS 'Order of steps (1, 2, 3, ...). Enforces deterministic ordering.';
COMMENT ON COLUMN room_directions.distance_meters IS 'Cumulative walking distance from start point to this step.';

-- ─── INDEXES FOR NAVIGATION QUERIES ──────────────────────────
-- Fast lookup: "Get all directions from MAIN_GATE to room X"
CREATE INDEX idx_room_directions_room_from
    ON room_directions (room_id, from_location_tag, step_order);

-- Dashboard query: "Show all from_location_tags available for room X"
CREATE INDEX idx_room_directions_room
    ON room_directions (room_id);
