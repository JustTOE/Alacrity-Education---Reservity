package dev.tmmc.reservity.reservations.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure helper that expands a single (startsAt, endsAt) seed into a list of
 * occurrences. M5 only handles WEEKLY with a fixed 12-occurrence horizon;
 * anything else is added in a future milestone.
 */
@Component
public class RecurrenceExpander {

    public static final int WEEKLY_OCCURRENCES = 12;

    /**
     * Returns 12 weekly occurrences starting at {@code seedStart}, each
     * preserving the same time-of-day. Occurrence 0 is the seed itself.
     */
    public List<Window> expandWeekly(Instant seedStart, Instant seedEnd) {
        if (seedStart == null || seedEnd == null) {
            throw new IllegalArgumentException("seedStart and seedEnd are required");
        }
        if (!seedEnd.isAfter(seedStart)) {
            throw new IllegalArgumentException("seedEnd must be after seedStart");
        }
        List<Window> windows = new ArrayList<>(WEEKLY_OCCURRENCES);
        for (int i = 0; i < WEEKLY_OCCURRENCES; i++) {
            Instant s = seedStart.plus(7L * i, ChronoUnit.DAYS);
            Instant e = seedEnd.plus(7L * i,   ChronoUnit.DAYS);
            windows.add(new Window(i, s, e));
        }
        return List.copyOf(windows);
    }

    public record Window(int index, Instant startsAt, Instant endsAt) {}
}
