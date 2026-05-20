package dev.tmmc.reservity.spaces.dto;

import dev.tmmc.reservity.spaces.availability.DayAvailability;

import java.util.UUID;

/**
 * Single-day, fatter payload for {@code GET /api/spaces/{slugOrId}/calendar?date=...}.
 * In M4 it's structurally identical to a single {@link DayAvailability} entry.
 * M5/M8 may extend it (per-reservation requester display name, attendees count,
 * full event description, etc.) without breaking the multi-day endpoint.
 */
public record CalendarDayResponse(
        UUID spaceId,
        String slug,
        String name,
        String zone,
        DayAvailability day
) {
}
