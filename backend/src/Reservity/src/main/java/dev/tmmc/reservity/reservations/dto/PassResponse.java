package dev.tmmc.reservity.reservations.dto;

import dev.tmmc.reservity.spaces.dto.SpaceResponse;

import java.time.Instant;
import java.util.UUID;

/**
 * The "rich pass" payload backing {@code GET /api/reservations/{id}/pass}.
 * Embeds the full {@link SpaceResponse} so BookingPass renders without an
 * extra space lookup.
 *
 * <p>{@code qrPayload} is the 64-char hex pass token. Treat as a secret —
 * served only to the reservation's owner (or admin).</p>
 */
public record PassResponse(
        UUID id,
        String reservationCode,
        SpaceResponse space,
        Instant startsAt,
        Instant endsAt,
        String qrPayload,
        boolean revoked,
        Instant checkinAt,
        Instant checkoutAt
) {}
