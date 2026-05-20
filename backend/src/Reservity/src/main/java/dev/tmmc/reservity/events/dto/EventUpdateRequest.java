package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Body for {@code PATCH /api/events/{id}}. All fields optional — null = leave
 * unchanged. Cannot change host or lifecycle status (cancel via /cancel).
 */
public record EventUpdateRequest(
        @Size(max = 160) String title,
        @Size(max = 400) String blurb,
        @Size(max = 4000) String description,
        @Size(max = 500) String coverImageUrl,

        Instant startsAt,
        Instant endsAt,

        UUID spaceId,
        UUID buildingId,
        @Size(max = 160) String locationLabel,

        EventCategory category,
        @Size(max = 40) String tag,

        @Min(0) Integer capacity,
        Boolean rsvpRequired,
        EventVisibility visibility
) {}
