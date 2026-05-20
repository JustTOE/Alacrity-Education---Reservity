package dev.tmmc.reservity.messaging.event;

import java.util.UUID;

/**
 * Published after a message is committed. The listener (M7
 * {@code MessageNotificationListener}) reloads entities by ID inside its
 * own transaction, so this event carries IDs only — never entity refs —
 * to avoid {@code LazyInitializationException}.
 *
 * <p>{@code contentPreview} is pre-truncated to 200 chars by the publisher
 * for use in notification bodies and email previews.
 */
public record MessageSentEvent(
        UUID conversationId,
        UUID messageId,
        UUID senderId,
        UUID recipientId,
        String contentPreview
) {}
