-- ============================================================================
-- V2: Users + auth-related tables
-- See plan §2.1 (Identity & access).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    email                    CITEXT       NOT NULL UNIQUE,
    email_verified_at        TIMESTAMPTZ,
    password_hash            VARCHAR(60)  NOT NULL,
    handle                   CITEXT       NOT NULL UNIQUE,
    display_name             VARCHAR(80)  NOT NULL,
    real_name                VARCHAR(120),
    initials                 VARCHAR(4)   NOT NULL,

    account_type             VARCHAR(16)  NOT NULL DEFAULT 'REQUESTER'
                             CHECK (account_type IN ('REQUESTER','OWNER','ADMIN')),
    verified_student         BOOLEAN      NOT NULL DEFAULT FALSE,

    bio                      VARCHAR(500),
    avatar_url               TEXT,
    cover_gradient           VARCHAR(120) NOT NULL DEFAULT 'linear-gradient(135deg,#4b52a7,#ff823c)',
    member_since             DATE         NOT NULL DEFAULT CURRENT_DATE,

    deleted_at               TIMESTAMPTZ,
    failed_login_attempts    INT          NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ,
    last_login_at            TIMESTAMPTZ,

    two_fa_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    two_fa_secret            VARCHAR(64),
    two_fa_backup_codes_left SMALLINT     NOT NULL DEFAULT 0,

    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT users_handle_format CHECK (handle ~ '^[a-z0-9_]{3,30}$'),
    CONSTRAINT users_initials_len  CHECK (char_length(initials) BETWEEN 1 AND 4)
);

CREATE INDEX idx_users_handle              ON users (handle);
CREATE INDEX idx_users_account_type        ON users (account_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_display_name_trgm   ON users USING gin (display_name gin_trgm_ops);

-- ----------------------------------------------------------------------------
-- user_settings — Appearance + notification preferences (one-to-one with user)
-- ----------------------------------------------------------------------------
CREATE TABLE user_settings (
    user_id                    UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

    theme                      VARCHAR(8)   NOT NULL DEFAULT 'auto'
                               CHECK (theme IN ('light','dark','sepia','auto')),
    accent                     VARCHAR(8)   NOT NULL DEFAULT 'indigo'
                               CHECK (accent IN ('indigo','plum','forest','rust','slate')),
    density                    VARCHAR(12)  NOT NULL DEFAULT 'comfortable'
                               CHECK (density IN ('cozy','comfortable','compact')),
    ornament_enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    reduce_motion              BOOLEAN      NOT NULL DEFAULT FALSE,
    high_contrast              BOOLEAN      NOT NULL DEFAULT FALSE,

    email_on_request_decided   BOOLEAN      NOT NULL DEFAULT TRUE,
    email_on_new_message       BOOLEAN      NOT NULL DEFAULT FALSE,
    email_on_event_reminder    BOOLEAN      NOT NULL DEFAULT TRUE,
    email_on_security_alert    BOOLEAN      NOT NULL DEFAULT TRUE,
    push_enabled               BOOLEAN      NOT NULL DEFAULT FALSE,
    quiet_hours_start          TIME,
    quiet_hours_end            TIME,

    locale                     VARCHAR(8)   NOT NULL DEFAULT 'en',
    timezone                   VARCHAR(64)  NOT NULL DEFAULT 'UTC',

    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- user_socials — Profile social links (github, linkedin, …)
-- ----------------------------------------------------------------------------
CREATE TABLE user_socials (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind          VARCHAR(24)  NOT NULL,
    handle        VARCHAR(120) NOT NULL,
    display_order SMALLINT     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    UNIQUE (user_id, kind)
);
CREATE INDEX idx_user_socials_user ON user_socials (user_id);

-- ----------------------------------------------------------------------------
-- user_backup_codes — TOTP backup codes (BCrypt-hashed)
-- ----------------------------------------------------------------------------
CREATE TABLE user_backup_codes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(60)  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_backup_codes_user ON user_backup_codes (user_id) WHERE used_at IS NULL;

-- ----------------------------------------------------------------------------
-- refresh_tokens — JWT refresh-token store (SHA-256 hash, with device metadata)
-- ----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    device_label VARCHAR(120),
    -- Stored as text to keep JDBC mapping simple. inet operators aren't needed here;
    -- the column is for display in the "active sessions" UI only.
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500),
    location     VARCHAR(120),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_expires     ON refresh_tokens (expires_at);

-- ----------------------------------------------------------------------------
-- email_verification_tokens
-- ----------------------------------------------------------------------------
CREATE TABLE email_verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    email       CITEXT       NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- password_reset_tokens
-- ----------------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
