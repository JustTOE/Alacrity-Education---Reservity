package dev.tmmc.reservity.events.dto;

import java.util.UUID;

/**
 * Pre-resolved location summary. The FE reads {@code spaceName} when present,
 * else {@code buildingName}, else {@code label}.
 */
public record EventLocation(
        UUID spaceId,
        String spaceSlug,
        String spaceName,
        UUID buildingId,
        String buildingName,
        String label
) {}
