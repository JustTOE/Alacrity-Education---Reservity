-- ============================================================================
-- V10: Drop the messaging feature (conversations, participants, messages) and
-- the user_settings.email_on_new_message preference that backed it. Feature is
-- no longer used by the product. V8 stays in history for Flyway checksum
-- continuity on existing environments.
-- ============================================================================

DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversation_participants;
DROP TABLE IF EXISTS conversations;

ALTER TABLE user_settings DROP COLUMN IF EXISTS email_on_new_message;
