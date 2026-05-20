package dev.tmmc.reservity.spaces.dto;

import java.time.Instant;
import java.util.UUID;

public record WaitlistResponse(
        UUID id,
        UUID spaceId,
        String spaceSlug,
        String spaceName,
        Instant desiredFrom,
        Instant desiredTo,
        Instant expiresAt,
        Instant createdAt
) {}
