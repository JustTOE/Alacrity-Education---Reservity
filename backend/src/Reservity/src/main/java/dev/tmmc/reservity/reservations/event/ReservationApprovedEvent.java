package dev.tmmc.reservity.reservations.event;

import java.util.UUID;

/**
 * Published when a reservation is created (instant-book or owner-approve path).
 * No listener in M5 — the M6 milestone wires {@code NotificationService} to
 * fan out an in-app + email notification.
 */
public record ReservationApprovedEvent(
        UUID reservationId,
        UUID requesterId,
        UUID spaceId
) {}
