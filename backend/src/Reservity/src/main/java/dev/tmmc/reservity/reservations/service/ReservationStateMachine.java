package dev.tmmc.reservity.reservations.service;

import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import org.springframework.stereotype.Component;

/**
 * Pure validator for the reservation lifecycle:
 *
 * <pre>
 *   PENDING ──┬─► APPROVED
 *             ├─► DENIED        (terminal)
 *             ├─► CANCELLED     (requester drops before approval)
 *             └─► EXPIRED       (background job — not in M5)
 *   APPROVED ──► CANCELLED      (requester drops a confirmed booking)
 * </pre>
 *
 * Anything else throws {@link IllegalStatusTransitionException} → HTTP 409.
 */
@Component
public class ReservationStateMachine {

    public void requireTransition(ReservationStatus from, Action action) {
        if (!isAllowed(from, action)) {
            throw new IllegalStatusTransitionException(
                    "Cannot " + action.name() + " a " + from + " request");
        }
    }

    public boolean isAllowed(ReservationStatus from, Action action) {
        return switch (action) {
            case APPROVE -> from == ReservationStatus.PENDING;
            case DENY    -> from == ReservationStatus.PENDING;
            case CANCEL  -> from == ReservationStatus.PENDING || from == ReservationStatus.APPROVED;
            case EXPIRE  -> from == ReservationStatus.PENDING;
        };
    }

    public ReservationStatus next(ReservationStatus from, Action action) {
        requireTransition(from, action);
        return switch (action) {
            case APPROVE -> ReservationStatus.APPROVED;
            case DENY    -> ReservationStatus.DENIED;
            case CANCEL  -> ReservationStatus.CANCELLED;
            case EXPIRE  -> ReservationStatus.EXPIRED;
        };
    }

    public enum Action {
        APPROVE, DENY, CANCEL, EXPIRE
    }
}
