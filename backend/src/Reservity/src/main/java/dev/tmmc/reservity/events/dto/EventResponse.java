package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.entity.enums.EventStatus;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Full event response. {@code displayStatus} is computed at mapping time, not
 * stored. {@code rsvpStatus} is the caller's RSVP if any (null when anonymous
 * or not RSVP'd).
 */
public record EventResponse(
        UUID id,
        String slug,

        EventHostSummary host,
        EventLocation location,
        UUID reservationId,

        Instant startsAt,
        Instant endsAt,

        String title,
        String blurb,
        String description,
        String coverImageUrl,

        EventCategory category,
        String tag,

        int capacity,
        int attendeesCount,
        boolean rsvpRequired,

        EventVisibility visibility,
        EventStatus lifecycleStatus,
        EventDisplayStatus displayStatus,

        RsvpStatus rsvpStatus,

        Instant cancelledAt,
        String cancellationReason,

        Instant createdAt,
        Instant updatedAt
) {}
