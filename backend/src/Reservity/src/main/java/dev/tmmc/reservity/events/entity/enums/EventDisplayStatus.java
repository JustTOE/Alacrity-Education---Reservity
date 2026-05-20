package dev.tmmc.reservity.events.entity.enums;

/**
 * Computed display bucket: not stored. Derived from
 * {@code (now, startsAt, endsAt)} in
 * {@link dev.tmmc.reservity.events.service.EventDisplayStatusCalculator}.
 */
public enum EventDisplayStatus {
    LIVE,
    SOON,
    TOMORROW,
    UPCOMING,
    PAST
}
