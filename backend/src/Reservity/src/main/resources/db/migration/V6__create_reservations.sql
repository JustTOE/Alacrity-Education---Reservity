-- ============================================================================
-- V6: Reservation lifecycle — series, requests, reservations, walk-in passes,
--     saved passes. Plus spaces.instant_book to drive auto-approve at submit.
-- See plan §2.4 / §M5-NOW.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- spaces.instant_book — when TRUE (default), POST /reservation-requests
-- auto-approves on submit and immediately creates a reservation + pass token.
-- When FALSE, the request stays PENDING until an owner approves via M9 UI.
-- ----------------------------------------------------------------------------
ALTER TABLE spaces
    ADD COLUMN instant_book BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_spaces_instant_book
    ON spaces (instant_book) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- reservation_series — recurring weekly/biweekly bookings.
-- M5 only generates WEEKLY series with a fixed 12-occurrence horizon.
-- BIWEEKLY is allowed by the CHECK for forward-compat but unused.
-- ----------------------------------------------------------------------------
CREATE TABLE reservation_series (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id        UUID         NOT NULL REFERENCES spaces(id) ON DELETE RESTRICT,
    requester_id    UUID         NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    pattern         VARCHAR(8)   NOT NULL CHECK (pattern IN ('WEEKLY','BIWEEKLY')),
    starts_on_date  DATE         NOT NULL,
    ends_on_date    DATE,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT series_time_order CHECK (end_time > start_time)
);
CREATE INDEX idx_series_requester ON reservation_series (requester_id);
CREATE INDEX idx_series_space     ON reservation_series (space_id);

-- ----------------------------------------------------------------------------
-- reservation_requests — every booking starts here.
--   * reservation_code is the human-readable id shown on the pass ("RV-LAB4B-3829").
--     Generated server-side; UNIQUE so duplicates are caught at the DB layer.
--   * duration_hours is GENERATED so analytics can SUM(duration_hours) cheaply.
--   * series_id + series_occurrence_idx tie a row to its weekly parent (NULL for one-offs).
--   * status state machine: PENDING -> APPROVED | DENIED | CANCELLED | EXPIRED.
--     The decided_at CHECK enforces the temporal invariant.
-- ----------------------------------------------------------------------------
CREATE TABLE reservation_requests (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_code      VARCHAR(20)  NOT NULL UNIQUE,
    space_id              UUID         NOT NULL REFERENCES spaces(id) ON DELETE RESTRICT,
    requester_id          UUID         NOT NULL REFERENCES users(id)  ON DELETE CASCADE,

    starts_at             TIMESTAMPTZ  NOT NULL,
    ends_at               TIMESTAMPTZ  NOT NULL,
    duration_hours        NUMERIC(4,2) GENERATED ALWAYS AS
                          (EXTRACT(EPOCH FROM (ends_at - starts_at)) / 3600.0) STORED,

    series_id             UUID         REFERENCES reservation_series(id) ON DELETE SET NULL,
    series_occurrence_idx INT,

    purpose               VARCHAR(500),
    attendees_count       SMALLINT     NOT NULL DEFAULT 1
                          CHECK (attendees_count BETWEEN 1 AND 1000),
    contact_phone         VARCHAR(40),
    tag                   VARCHAR(20)  NOT NULL DEFAULT 'Solo focus'
                          CHECK (tag IN ('Solo focus','Group session','Drop-in','Event','Other')),

    status                VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','APPROVED','DENIED','CANCELLED','EXPIRED')),
    auto_approved         BOOLEAN      NOT NULL DEFAULT FALSE,
    decided_at            TIMESTAMPTZ,
    decided_by            UUID         REFERENCES users(id) ON DELETE SET NULL,
    rejection_reason      VARCHAR(500),
    cancelled_at          TIMESTAMPTZ,
    cancelled_by          UUID         REFERENCES users(id) ON DELETE SET NULL,
    cancellation_reason   VARCHAR(500),

    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT request_window_order CHECK (ends_at > starts_at),
    CONSTRAINT request_decided_when_terminal CHECK (
        (status IN ('APPROVED','DENIED') AND decided_at IS NOT NULL)
        OR status NOT IN ('APPROVED','DENIED')
    )
);

CREATE INDEX idx_reservation_req_space_time ON reservation_requests (space_id, starts_at);
CREATE INDEX idx_reservation_req_requester  ON reservation_requests (requester_id, starts_at DESC);
CREATE INDEX idx_reservation_req_status     ON reservation_requests (status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_reservation_req_series     ON reservation_requests (series_id) WHERE series_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- reservations — the blocking table. Created only when a request is APPROVED.
--   * 1:1 with reservation_requests via UNIQUE(request_id).
--   * pass_token is the QR payload. 64-char hex == 256 random bits.
--   * EXCLUDE USING gist (...) is the source of truth for "no two bookings
--     overlap on the same space". Race winner: first commit wins.
-- ----------------------------------------------------------------------------
CREATE TABLE reservations (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id    UUID         NOT NULL UNIQUE REFERENCES reservation_requests(id) ON DELETE RESTRICT,
    space_id      UUID         NOT NULL REFERENCES spaces(id) ON DELETE RESTRICT,
    user_id       UUID         NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
    starts_at     TIMESTAMPTZ  NOT NULL,
    ends_at       TIMESTAMPTZ  NOT NULL,
    checkin_at    TIMESTAMPTZ,
    checkout_at   TIMESTAMPTZ,
    no_show       BOOLEAN      NOT NULL DEFAULT FALSE,
    pass_token    VARCHAR(64)  NOT NULL UNIQUE,
    pass_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT reservation_window_order CHECK (ends_at > starts_at)
);

CREATE INDEX idx_reservations_user      ON reservations (user_id, starts_at DESC);
CREATE INDEX idx_reservations_space     ON reservations (space_id, starts_at);

-- The non-overlap safety net (plan §2.4). [) bound: end is exclusive so a
-- 14:00–16:00 booking does not collide with a 16:00–18:00 booking.
ALTER TABLE reservations
    ADD CONSTRAINT no_overlap_per_space
    EXCLUDE USING gist (space_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&);

-- ----------------------------------------------------------------------------
-- walk_in_passes — table only in M5; controller is M10.
-- ----------------------------------------------------------------------------
CREATE TABLE walk_in_passes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    space_id    UUID         NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    pass_token  VARCHAR(64)  NOT NULL UNIQUE,
    starts_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ends_at     TIMESTAMPTZ  NOT NULL,
    used_count  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT walkin_window_order CHECK (ends_at > starts_at)
);
CREATE INDEX idx_walkin_user   ON walk_in_passes (user_id);
CREATE INDEX idx_walkin_active ON walk_in_passes (space_id, ends_at);

-- ----------------------------------------------------------------------------
-- saved_passes — every confirmed reservation auto-pins to its requester so
-- "Recent passes" survives session/refresh. Composite PK prevents duplicates.
-- ----------------------------------------------------------------------------
CREATE TABLE saved_passes (
    user_id        UUID        NOT NULL REFERENCES users(id)        ON DELETE CASCADE,
    reservation_id UUID        NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    saved_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, reservation_id)
);
CREATE INDEX idx_saved_passes_user ON saved_passes (user_id, saved_at DESC);
