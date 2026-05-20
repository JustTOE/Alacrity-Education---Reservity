-- ============================================================================
-- V4: Buildings + vibes (reference tables for the Spaces domain)
-- See plan §2.2.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- buildings
-- Names + short codes are unique. pin_x / pin_y are 0..1 coords on the
-- frontend's CampusMap SVG; latitude / longitude are real GPS for any
-- mapping integration later.
-- ----------------------------------------------------------------------------
CREATE TABLE buildings (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(120) NOT NULL UNIQUE,
    short_code      VARCHAR(8)   NOT NULL UNIQUE,
    campus_name     VARCHAR(80),
    address         VARCHAR(240),
    latitude        NUMERIC(9,6),
    longitude       NUMERIC(9,6),
    pin_x           NUMERIC(5,4),
    pin_y           NUMERIC(5,4),
    hero_image_url  TEXT,
    description     VARCHAR(2000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT buildings_short_code_format CHECK (short_code ~ '^[A-Z0-9]{2,8}$'),
    CONSTRAINT buildings_pin_x_range CHECK (pin_x IS NULL OR (pin_x >= 0 AND pin_x <= 1)),
    CONSTRAINT buildings_pin_y_range CHECK (pin_y IS NULL OR (pin_y >= 0 AND pin_y <= 1))
);

CREATE INDEX idx_buildings_campus ON buildings (campus_name);

-- ----------------------------------------------------------------------------
-- vibes
-- Lookup table whose primary key is the human-meaningful slug used by the
-- frontend (e.g. "focus", "lab-only"). The 8 canonical entries are seeded by
-- R__seed_vibes.sql so changes can be made by editing one file.
-- ----------------------------------------------------------------------------
CREATE TABLE vibes (
    id            VARCHAR(20)  PRIMARY KEY,
    label         VARCHAR(40)  NOT NULL,
    icon          VARCHAR(8)   NOT NULL,
    description   VARCHAR(200),
    display_order SMALLINT     NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT vibes_id_format CHECK (id ~ '^[a-z][a-z0-9-]{1,19}$')
);

CREATE INDEX idx_vibes_display_order ON vibes (display_order) WHERE active = TRUE;
