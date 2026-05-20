package dev.tmmc.reservity.reservations.dto;

import dev.tmmc.reservity.spaces.dto.SpaceSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors {@code ReservationRequest} for owner-queue and "/mine" listings.
 * Lighter than {@link ReservationResponse} — no {@code passToken} (owners
 * don't see other users' QR payloads).
 */
public record ReservationRequestResponse(
        UUID id,
        String reservationCode,
        SpaceSummary space,
        UUID requesterId,
        String requesterDisplayName,
        Instant startsAt,
        Instant endsAt,
        String recurring,                  // "none" | "weekly" — derived from seriesId
        UUID seriesId,
        Integer seriesOccurrenceIdx,
        String purpose,
        short attendeesCount,
        String contactPhone,
        String tag,
        String status,
        boolean autoApproved,
        Instant decidedAt,
        UUID decidedById,
        String rejectionReason,
        Instant cancelledAt,
        UUID cancelledById,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {}
