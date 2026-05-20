package dev.tmmc.reservity.spaces.dto;

import dev.tmmc.reservity.spaces.availability.DayAvailability;

import java.time.LocalDate;
import java.util.Map;

/**
 * Map keyed by slug (caller's preferred lookup key). When the caller submitted
 * {@code spaceIds} instead of {@code slugs}, the slug is still resolved per
 * space so the frontend can index the map by slug consistently.
 */
public record BatchAvailabilityResponse(
        LocalDate date,
        String zone,
        Map<String, DayAvailability> spaces
) {
}
