package dev.tmmc.reservity.spaces.dto;

import dev.tmmc.reservity.spaces.availability.DayAvailability;

import java.util.List;
import java.util.UUID;

/**
 * Multi-day availability payload for {@code GET /api/spaces/{slugOrId}/availability}.
 *
 * <p>Each {@link DayAvailability} carries the full 24-hour state grid plus the
 * frontend-shaped {@code booked: int[]} list for backward compatibility with
 * the existing {@code bookedForDate} renderer.
 */
public record AvailabilityResponse(
        UUID spaceId,
        String slug,
        String name,
        String zone,
        List<DayAvailability> days
) {
}
