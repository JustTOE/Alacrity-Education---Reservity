package dev.tmmc.reservity.reservations.entity;

/**
 * Recurrence pattern for {@code reservation_series}. M5 only generates
 * {@link #WEEKLY} series; {@link #BIWEEKLY} is allowed by the DB CHECK for
 * forward-compat but ignored by {@code RecurrenceExpander}.
 */
public enum RecurrencePattern {
    WEEKLY,
    BIWEEKLY
}
