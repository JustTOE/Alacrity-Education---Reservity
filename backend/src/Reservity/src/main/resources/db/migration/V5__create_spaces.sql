-- ============================================================================
-- V5: Spaces, images, vibe joins, favorites, waitlist, closures
-- See plan §2.2 / §2.3.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- spaces
--   * slug doubles as the human-readable URL identifier ("/spaces/{slug}").
--   * Polymorphic owner: owner_type IN ('USER','ORGANIZATION'); the FK is
--     application-enforced (no DB constraint to either table) since referential
--     integrity across two tables can't be expressed cleanly without triggers.
--   * type matches the frontend's SpaceType union ('lab','pod','open','studio').
--   * amenities, rules, operating_hours are JSONB so the frontend's string[] /
--     {mode,...} shapes round-trip without a join table.
--   * is_free is a generated column — saves "free" filter queries from
--     comparing price_per_hour every time.
-- ----------------------------------------------------------------------------
CREATE TABLE spaces (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(80)  NOT NULL UNIQUE,

    owner_id        UUID         NOT NULL,
    owner_type      VARCHAR(12)  NOT NULL CHECK (owner_type IN ('USER','ORGANIZATION')),

    name            VARCHAR(120) NOT NULL,
    type            VARCHAR(16)  NOT NULL CHECK (type IN ('lab','pod','open','studio')),

    building_id     UUID         NOT NULL REFERENCES buildings(id) ON DELETE RESTRICT,
    floor           SMALLINT     NOT NULL,
    room            VARCHAR(20)  NOT NULL,

    seats           SMALLINT     NOT NULL CHECK (seats >= 1 AND seats <= 1000),
    area_sqm        NUMERIC(7,2) NOT NULL CHECK (area_sqm > 0),

    price_per_hour  NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price_per_hour >= 0),
    currency        VARCHAR(8)    NOT NULL DEFAULT 'USD',
    is_free         BOOLEAN       GENERATED ALWAYS AS (price_per_hour = 0) STORED,

    blurb           VARCHAR(140) NOT NULL,
    description     VARCHAR(2000),
    amenities       JSONB        NOT NULL DEFAULT '[]'::JSONB,
    rules           JSONB        NOT NULL DEFAULT '[]'::JSONB,

    operating_hours JSONB        NOT NULL DEFAULT '{"mode":"24/7"}'::JSONB,

    pin_x           NUMERIC(5,4),
    pin_y           NUMERIC(5,4),
    latitude        NUMERIC(9,6),
    longitude       NUMERIC(9,6),

    status          VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED'
                    CHECK (status IN ('DRAFT','PENDING_REVIEW','PUBLISHED','SUSPENDED','ARCHIVED')),
    surprise        BOOLEAN      NOT NULL DEFAULT FALSE,
    drop_in         BOOLEAN      NOT NULL DEFAULT FALSE,

    view_count      INT          NOT NULL DEFAULT 0,
    rating_avg      NUMERIC(3,2),
    rating_count    INT          NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT spaces_slug_format    CHECK (slug ~ '^[a-z0-9-]{2,80}$'),
    CONSTRAINT spaces_pin_x_range    CHECK (pin_x IS NULL OR (pin_x >= 0 AND pin_x <= 1)),
    CONSTRAINT spaces_pin_y_range    CHECK (pin_y IS NULL OR (pin_y >= 0 AND pin_y <= 1))
);

CREATE INDEX idx_spaces_owner       ON spaces (owner_type, owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_spaces_building    ON spaces (building_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_spaces_type        ON spaces (type) WHERE deleted_at IS NULL;
CREATE INDEX idx_spaces_status      ON spaces (status);
CREATE INDEX idx_spaces_published   ON spaces (created_at) WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE INDEX idx_spaces_name_trgm   ON spaces USING gin (name gin_trgm_ops);
CREATE INDEX idx_spaces_blurb_trgm  ON spaces USING gin (blurb gin_trgm_ops);
CREATE INDEX idx_spaces_amenities   ON spaces USING gin (amenities);

CREATE INDEX idx_spaces_price       ON spaces (price_per_hour) WHERE deleted_at IS NULL;
CREATE INDEX idx_spaces_capacity    ON spaces (seats) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- space_images — at most one primary image per space.
-- M2 stores rows but does not handle binary upload (that is M3 — multipart
-- through StorageService). Until then, url is whatever string the seed inserts.
-- ----------------------------------------------------------------------------
CREATE TABLE space_images (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id      UUID         NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    s3_key        VARCHAR(500) NOT NULL UNIQUE,
    url           TEXT         NOT NULL,
    alt_text      VARCHAR(240),
    width         INT,
    height        INT,
    byte_size     BIGINT,
    content_type  VARCHAR(40),
    display_order SMALLINT     NOT NULL DEFAULT 0,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_space_images_space ON space_images (space_id, display_order);
CREATE UNIQUE INDEX uq_space_image_primary
    ON space_images (space_id) WHERE is_primary = TRUE;

-- ----------------------------------------------------------------------------
-- space_vibes — many-to-many join.
-- ----------------------------------------------------------------------------
CREATE TABLE space_vibes (
    space_id  UUID        NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    vibe_id   VARCHAR(20) NOT NULL REFERENCES vibes(id)  ON DELETE RESTRICT,
    PRIMARY KEY (space_id, vibe_id)
);
CREATE INDEX idx_space_vibes_vibe ON space_vibes (vibe_id);

-- ----------------------------------------------------------------------------
-- space_favorites — composite PK so the same user can't double-favorite.
-- ----------------------------------------------------------------------------
CREATE TABLE space_favorites (
    user_id     UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    space_id    UUID        NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, space_id)
);
CREATE INDEX idx_space_favorites_space ON space_favorites (space_id);

-- ----------------------------------------------------------------------------
-- space_waitlist — "Notify me when free".
-- desired_from / desired_to may be null (== "any opening").
-- expires_at defaults to 14 days from creation; the watcher removes stale rows.
-- ----------------------------------------------------------------------------
CREATE TABLE space_waitlist (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    space_id        UUID         NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    desired_from    TIMESTAMPTZ,
    desired_to      TIMESTAMPTZ,
    notified_at     TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW() + INTERVAL '14 days',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT waitlist_window_order CHECK (
        desired_from IS NULL OR desired_to IS NULL OR desired_to > desired_from
    )
);

-- Postgres treats NULLs as distinct under UNIQUE by default, which prevents
-- "any opening" duplicates. Use NULLS NOT DISTINCT so two rows with both
-- timestamps NULL collide.
CREATE UNIQUE INDEX uq_waitlist_user_space_window
    ON space_waitlist (user_id, space_id, desired_from, desired_to)
    NULLS NOT DISTINCT;

CREATE INDEX idx_waitlist_active ON space_waitlist (space_id) WHERE notified_at IS NULL;

-- ----------------------------------------------------------------------------
-- space_closures — owner-set blocks (maintenance, holidays). Distinct from
-- operating_hours (recurring) and reservations (booked time).
-- ----------------------------------------------------------------------------
CREATE TABLE space_closures (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id    UUID         NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    starts_at   TIMESTAMPTZ  NOT NULL,
    ends_at     TIMESTAMPTZ  NOT NULL,
    reason      VARCHAR(200),
    created_by  UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT closures_window_order CHECK (ends_at > starts_at)
);
CREATE INDEX idx_closures_space_time ON space_closures (space_id, starts_at, ends_at);
