package dev.tmmc.reservity.spaces.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/spaces/availability:batch}. Supply either {@code spaceIds}
 * <em>or</em> {@code slugs} (not both, not neither). Capped at 50 entries to
 * keep response sizes bounded.
 */
public record BatchAvailabilityRequest(
        List<UUID> spaceIds,
        List<String> slugs,
        @NotNull LocalDate date
) {
}
