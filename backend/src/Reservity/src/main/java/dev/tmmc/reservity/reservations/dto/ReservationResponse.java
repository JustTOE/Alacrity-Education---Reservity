package dev.tmmc.reservity.reservations.dto;

import dev.tmmc.reservity.spaces.dto.SpaceSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * Carries everything the frontend's {@code ConfirmedBooking} interface needs
 * plus the M5 superset (status, passToken, decidedAt). When status=PENDING
 * (instant_book=false flow), {@code reservationId} and {@code passToken} are
 * null — the frontend would render a "request submitted" state.
 *
 * <p>The {@code id} field is the human-readable reservation_code
 * ({@code "RV-LAB4B-839271"}). The frontend currently generates this
 * client-side; M5 owns it and returns it here.</p>
 */
public record ReservationResponse(
        String id,                         // reservation_code
        UUID requestId,
        UUID reservationId,                // null when status=PENDING
        SpaceSummary space,
        Instant startsAt,
        Instant endsAt,
        String status,                     // ReservationStatus name
        boolean autoApproved,
        String passToken,                  // null unless requester is current user AND approved
        Instant createdAt,
        Instant decidedAt,
        String recurring,                  // "none" | "weekly"
        UUID seriesId,
        Integer seriesOccurrences          // 12 for weekly responses to the seed; null for one-offs
) {}
