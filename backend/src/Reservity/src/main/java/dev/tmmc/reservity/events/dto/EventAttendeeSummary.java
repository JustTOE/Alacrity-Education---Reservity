package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.RsvpStatus;

import java.time.Instant;
import java.util.UUID;

public record EventAttendeeSummary(
        UUID userId,
        String handle,
        String displayName,
        RsvpStatus status,
        Instant joinedAt
) {}
