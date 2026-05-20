package dev.tmmc.reservity.events.entity.enums;

/**
 * Stored lifecycle status. Distinct from {@link EventDisplayStatus},
 * which is computed from time at response-mapping time.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    PAST
}
