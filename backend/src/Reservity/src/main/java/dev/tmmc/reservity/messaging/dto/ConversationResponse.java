package dev.tmmc.reservity.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID requestId,
        ConversationParticipantSummary otherParticipant,
        MessagePreview lastMessage,
        long unreadCount,
        Instant updatedAt,
        Instant createdAt
) {}
