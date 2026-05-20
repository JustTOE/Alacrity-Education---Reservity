package dev.tmmc.reservity.messaging.dto;

import java.util.UUID;

public record ConversationParticipantSummary(
        UUID id,
        String displayName,
        String handle
) {}
