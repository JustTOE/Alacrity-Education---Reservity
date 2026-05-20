package dev.tmmc.reservity.spaces.availability;

import java.util.UUID;

/**
 * A pre-resolved event or reservation that occupies one or more hour slots
 * within a single day. M4 always supplies an empty list — M5 (reservations)
 * and M8 (events) populate it.
 */
public record EventBlock(
        Source source,
        UUID id,
        int startH,
        int endH,
        String title,
        String host
) {
    public enum Source { RESERVATION, EVENT }
}
