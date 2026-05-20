package dev.tmmc.reservity.spaces.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lighter payload for list / card views. Drops the long description, rules,
 * events, and bookings. Same id-shape as {@link SpaceResponse} (slug as id).
 */
public record SpaceSummary(
        String id,
        UUID uuid,
        String name,
        String type,
        String building,
        short seats,
        BigDecimal area,
        BigDecimal price,
        String currency,
        List<String> vibes,
        String blurb,
        SpaceResponse.Pin pin,
        Boolean surprise,
        boolean dropIn,
        boolean instantBook,
        String primaryImageUrl
) {}
