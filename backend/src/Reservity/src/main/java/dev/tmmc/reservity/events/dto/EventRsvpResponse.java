package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.RsvpStatus;

import java.time.Instant;
import java.util.UUID;

public record EventRsvpResponse(
        UUID eventId,
        UUID userId,
        String displayName,
        String handle,
        RsvpStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
