package dev.tmmc.reservity.reservations.entity;

/**
 * The lifecycle of a reservation request.
 *
 * <pre>
 *   PENDING ──┬─► APPROVED ──► CANCELLED  (requester cancels confirmed booking)
 *             ├─► DENIED                  (terminal)
 *             └─► CANCELLED               (requester drops before approval)
 *
 *   PENDING ──► EXPIRED                   (auto by background job — not in M5)
 * </pre>
 *
 * The {@code IllegalStatusTransitionException} thrown by
 * {@code ReservationStateMachine} is mapped to HTTP 409 by
 * {@code GlobalExceptionHandler}.
 */
public enum ReservationStatus {
    PENDING,
    APPROVED,
    DENIED,
    CANCELLED,
    EXPIRED
}
