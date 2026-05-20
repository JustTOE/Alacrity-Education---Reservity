package dev.tmmc.reservity.reservations.event;

import java.util.UUID;

/**
 * Published when an owner denies a pending request. M6 listener will email
 * the requester with the reason.
 */
public record ReservationDeniedEvent(
        UUID requestId,
        UUID requesterId,
        UUID spaceId,
        String reason
) {}
