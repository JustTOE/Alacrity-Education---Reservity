package dev.tmmc.reservity.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/** Slim shape for the conversation-list "last message" preview. */
public record MessagePreview(
        UUID id,
        UUID senderId,
        String content,
        Instant createdAt
) {}
