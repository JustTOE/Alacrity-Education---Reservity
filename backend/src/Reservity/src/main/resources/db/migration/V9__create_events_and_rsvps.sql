-- ============================================================================
-- V9: Events & RSVP. See plan §2.5 / §M8-NOW.
-- Two tables: events (the thing being held) and event_rsvps (the m:n with
-- users + status). attendees_count is denormalized on events for cheap reads
-- on the landing-page board; updates happen transactionally inside
-- EventRsvpService so the count never drifts from COUNT(GOING).
-- ============================================================================
CREATE TABLE events (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                VARCHAR(80)  NOT NULL UNIQUE,

    -- Polymorphic host: USER or ORGANIZATION (mirrors spaces.owner_type/id).
    host_id             UUID         NOT NULL,
    host_type           VARCHAR(12)  NOT NULL CHECK (host_type IN ('USER','ORGANIZATION')),

    -- Where: all three optional. UI prefers space → building → free-form label.
    space_id            UUID         REFERENCES spaces(id)        ON DELETE SET NULL,
    reservation_id      UUID         REFERENCES reservations(id)  ON DELETE SET NULL,
    building_id         UUID         REFERENCES buildings(id)     ON DELETE SET NULL,
    location_label      VARCHAR(160),

    -- When
    starts_at           TIMESTAMPTZ  NOT NULL,
    ends_at             TIMESTAMPTZ  NOT NULL,

    -- Display
    title               VARCHAR(160) NOT NULL,
    blurb               VARCHAR(400) NOT NULL,
    description         VARCHAR(4000),
    cover_image_url     TEXT,
    category            VARCHAR(20)  NOT NULL DEFAULT 'OTHER'
                        CHECK (category IN ('ACADEMIC','SOCIAL','PERFORMANCE','WORKSHOP','DROP_IN','TALK','SPORTS','OTHER')),
    tag                 VARCHAR(40),

    -- RSVP
    capacity            INT          NOT NULL DEFAULT 0,
    attendees_count     INT          NOT NULL DEFAULT 0,
    rsvp_required       BOOLEAN      NOT NULL DEFAULT FALSE,
    invite_only         BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Visibility & lifecycle
    visibility          VARCHAR(12)  NOT NULL DEFAULT 'PUBLIC'
                        CHECK (visibility IN ('PUBLIC','UNLISTED','PRIVATE')),
    status              VARCHAR(12)  NOT NULL DEFAULT 'PUBLISHED'
                        CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED','PAST')),

    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CHECK (ends_at > starts_at),
    CHECK (capacity >= 0),
    CHECK (attendees_count >= 0),
    CHECK (slug ~ '^[a-z0-9-]{2,80}$')
);

-- Hot path: landing board "what's happening today / soon".
CREATE INDEX idx_events_starts
    ON events (starts_at)
    WHERE status = 'PUBLISHED' AND visibility = 'PUBLIC';

CREATE INDEX idx_events_status_starts ON events (status, starts_at);
CREATE INDEX idx_events_space         ON events (space_id)    WHERE space_id IS NOT NULL;
CREATE INDEX idx_events_building      ON events (building_id) WHERE building_id IS NOT NULL;
CREATE INDEX idx_events_host          ON events (host_type, host_id);
CREATE INDEX idx_events_title_trgm    ON events USING gin (title gin_trgm_ops);

-- ============================================================================
CREATE TABLE event_rsvps (
    event_id    UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id     UUID         NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    status      VARCHAR(12)  NOT NULL DEFAULT 'GOING'
                CHECK (status IN ('GOING','WAITLIST','INTERESTED','CANT_GO','CANCELLED')),
    notes       VARCHAR(240),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    PRIMARY KEY (event_id, user_id)
);

-- "My events" hot path.
CREATE INDEX idx_event_rsvps_user
    ON event_rsvps (user_id, created_at DESC);

-- Auto-promote query: oldest WAITLIST for a given event.
CREATE INDEX idx_event_rsvps_waitlist
    ON event_rsvps (event_id, created_at)
    WHERE status = 'WAITLIST';
