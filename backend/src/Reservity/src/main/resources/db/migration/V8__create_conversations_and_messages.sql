-- ============================================================================
-- V8: Messaging. See plan §2.6 / §M7-NOW.
-- Three tables: conversations (the thread), conversation_participants (m:n
-- with per-user read receipts), messages (the body). Lazy-created — a
-- conversation only exists once someone calls POST /api/conversations.
-- request_id is nullable for forward-compat with free-form 1:1 DMs (not
-- exposed by the M7 controller).
-- ============================================================================
CREATE TABLE conversations (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id  UUID         REFERENCES reservation_requests(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- "Is there already a conversation for this request?" — drives the lazy
-- find-or-create lookup in MessageService.
CREATE INDEX idx_conversations_request
    ON conversations (request_id)
    WHERE request_id IS NOT NULL;

CREATE TABLE conversation_participants (
    conversation_id UUID         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_read_at    TIMESTAMPTZ,
    PRIMARY KEY (conversation_id, user_id)
);

-- "List my conversations" hot path.
CREATE INDEX idx_convo_participants_user
    ON conversation_participants (user_id);

CREATE TABLE messages (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID          NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID          NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    content         VARCHAR(4000) NOT NULL,
    edited_at       TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (char_length(trim(content)) > 0)
);

-- Thread feed: paginated by created_at DESC; the "before=" cursor on the
-- list endpoint reads the leading edge of this index.
CREATE INDEX idx_messages_convo_time
    ON messages (conversation_id, created_at DESC);
