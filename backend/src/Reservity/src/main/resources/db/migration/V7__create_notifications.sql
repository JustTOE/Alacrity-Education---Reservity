-- ============================================================================
-- V7: Notifications. See plan §2.6 / §M6-NOW.
-- One row per user-facing notification (currently channel=IN_APP only).
-- Email sends are NOT persisted as rows in M6 — success/failure is logged
-- inside EmailService. The PUSH channel is reserved for a future milestone.
-- ============================================================================
CREATE TABLE notifications (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(40)  NOT NULL,
    channel       VARCHAR(8)   NOT NULL
                  CHECK (channel IN ('IN_APP','EMAIL','PUSH')),
    title         VARCHAR(160) NOT NULL,
    body          VARCHAR(800) NOT NULL,
    link_url      VARCHAR(500),
    related_type  VARCHAR(24),
    related_id    UUID,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Hot path: bell-icon unread badge (cheap COUNT) + drawer unread feed.
CREATE INDEX idx_notif_user_unread
    ON notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;

-- Full feed (read + unread) ordering — separate index because the partial
-- above can't serve queries that don't filter on read_at IS NULL.
CREATE INDEX idx_notif_user
    ON notifications (user_id, created_at DESC);
