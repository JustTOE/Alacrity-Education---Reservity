-- Postgres extensions used across the schema.
-- pgcrypto:    gen_random_uuid() for UUID PKs.
-- btree_gist:  required for the EXCLUDE USING gist (...) constraint that prevents overlapping reservations.
-- citext:      case-insensitive email + handle uniqueness.
-- pg_trgm:     trigram GIN indexes for fuzzy text search on space/event/user names.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
