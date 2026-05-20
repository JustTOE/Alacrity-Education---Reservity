package dev.tmmc.reservity.reservations;

import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import dev.tmmc.reservity.reservations.service.ReservationStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit test — no Spring context. */
class ReservationStateMachineTest {

    private final ReservationStateMachine sm = new ReservationStateMachine();

    @Test
    void approve_only_allowed_from_pending() {
        assertEquals(ReservationStatus.APPROVED,
                sm.next(ReservationStatus.PENDING, ReservationStateMachine.Action.APPROVE));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.APPROVED, ReservationStateMachine.Action.APPROVE));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.DENIED, ReservationStateMachine.Action.APPROVE));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.CANCELLED, ReservationStateMachine.Action.APPROVE));
    }

    @Test
    void deny_only_allowed_from_pending() {
        assertEquals(ReservationStatus.DENIED,
                sm.next(ReservationStatus.PENDING, ReservationStateMachine.Action.DENY));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.APPROVED, ReservationStateMachine.Action.DENY));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.DENIED, ReservationStateMachine.Action.DENY));
    }

    @Test
    void cancel_allowed_from_pending_and_approved() {
        assertEquals(ReservationStatus.CANCELLED,
                sm.next(ReservationStatus.PENDING, ReservationStateMachine.Action.CANCEL));
        assertEquals(ReservationStatus.CANCELLED,
                sm.next(ReservationStatus.APPROVED, ReservationStateMachine.Action.CANCEL));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.DENIED, ReservationStateMachine.Action.CANCEL));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.CANCELLED, ReservationStateMachine.Action.CANCEL));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.EXPIRED, ReservationStateMachine.Action.CANCEL));
    }

    @Test
    void expire_only_allowed_from_pending() {
        assertEquals(ReservationStatus.EXPIRED,
                sm.next(ReservationStatus.PENDING, ReservationStateMachine.Action.EXPIRE));
        assertThrows(IllegalStatusTransitionException.class,
                () -> sm.next(ReservationStatus.APPROVED, ReservationStateMachine.Action.EXPIRE));
    }

    @Test
    void isAllowed_does_not_throw_on_illegal_edge() {
        assertFalse(sm.isAllowed(ReservationStatus.DENIED, ReservationStateMachine.Action.APPROVE));
        assertTrue(sm.isAllowed(ReservationStatus.PENDING, ReservationStateMachine.Action.APPROVE));
    }
}
