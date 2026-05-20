package dev.tmmc.reservity.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderDisplayName,
        String content,
        Instant editedAt,
        Instant createdAt
) {}
