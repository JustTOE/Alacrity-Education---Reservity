-- ============================================================================
-- V3: Organizations + memberships
-- See plan §2.1.
-- ============================================================================

CREATE TABLE organizations (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(60)  NOT NULL UNIQUE,
    name           VARCHAR(120) NOT NULL,
    short_name     VARCHAR(40),
    description    VARCHAR(2000),
    website_url    TEXT,
    logo_url       TEXT,
    cover_gradient VARCHAR(120),
    org_type       VARCHAR(16)  NOT NULL DEFAULT 'OTHER'
                   CHECK (org_type IN ('FACULTY','DEPARTMENT','SOCIETY','CLUB','LIBRARY','OTHER')),
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT orgs_slug_format CHECK (slug ~ '^[a-z0-9-]{2,60}$')
);
CREATE INDEX idx_orgs_org_type   ON organizations (org_type);
CREATE INDEX idx_orgs_name_trgm  ON organizations USING gin (name gin_trgm_ops);

-- ----------------------------------------------------------------------------
-- org_memberships — composite PK (user_id, org_id)
-- ----------------------------------------------------------------------------
CREATE TABLE org_memberships (
    user_id    UUID         NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    org_id     UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role       VARCHAR(8)   NOT NULL DEFAULT 'MEMBER'
               CHECK (role IN ('OWNER','ADMIN','MEMBER')),
    invited_by UUID         REFERENCES users(id) ON DELETE SET NULL,
    joined_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, org_id)
);
CREATE INDEX idx_org_memb_org ON org_memberships (org_id);

-- An org may have many ADMINs but exactly one OWNER. Enforce via partial unique index.
CREATE UNIQUE INDEX uq_org_single_owner
    ON org_memberships (org_id) WHERE role = 'OWNER';
