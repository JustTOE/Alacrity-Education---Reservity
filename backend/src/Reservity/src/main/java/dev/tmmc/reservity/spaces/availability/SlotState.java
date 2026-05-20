package dev.tmmc.reservity.spaces.availability;

public enum SlotState {
    /** Outside operating hours or closure-overridden — render greyed out. */
    CLOSED,
    /** Within operating hours and free — bookable. */
    OPEN,
    /** Held by an approved reservation (M5+). */
    RESERVED,
    /** Held by a published event (M8+). */
    EVENT
}
