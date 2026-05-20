package dev.tmmc.reservity.spaces.availability;

import java.util.UUID;

public record HourSlot(
        int hour,
        SlotState state,
        String label,
        UUID eventId,
        UUID reservationId
) {
    public static HourSlot closed(int hour) {
        return new HourSlot(hour, SlotState.CLOSED, null, null, null);
    }

    public static HourSlot open(int hour) {
        return new HourSlot(hour, SlotState.OPEN, null, null, null);
    }

    public static HourSlot event(int hour, String title, UUID eventId) {
        return new HourSlot(hour, SlotState.EVENT, title, eventId, null);
    }

    public static HourSlot reserved(int hour, String title, UUID reservationId) {
        return new HourSlot(hour, SlotState.RESERVED, title, null, reservationId);
    }
}
