package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Body for {@code POST /api/events}. The host pair is mandatory; location
 * fields are optional, but at least one ought to be set so the UI has
 * something to render. Validation is service-side (caller permission +
 * resource existence) rather than purely declarative.
 */
public record EventCreateRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 400) String blurb,
        @Size(max = 4000) String description,
        @Size(max = 500) String coverImageUrl,

        @NotNull Instant startsAt,
        @NotNull Instant endsAt,

        @NotNull EventHostType hostType,
        UUID hostId,

        UUID spaceId,
        UUID reservationId,
        UUID buildingId,
        @Size(max = 160) String locationLabel,

        EventCategory category,
        @Size(max = 40) String tag,

        @Min(0) Integer capacity,
        Boolean rsvpRequired,
        EventVisibility visibility
) {}
