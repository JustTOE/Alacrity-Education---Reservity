package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.entity.enums.EventStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Slim event row for list endpoints (landing-page board, "my events").
 * Mirrors the frontend's {@code CommunityEvent} interface so the FE swap
 * is a thin adapter.
 */
public record EventSummary(
        UUID id,
        String slug,
        String title,
        String blurb,
        String hostName,
        String hostInitials,
        String spaceName,
        String buildingName,
        Instant startsAt,
        Instant endsAt,
        EventCategory category,
        String tag,
        int attendeesCount,
        int capacity,
        EventDisplayStatus displayStatus,
        EventStatus lifecycleStatus,
        String coverImageUrl
) {}
