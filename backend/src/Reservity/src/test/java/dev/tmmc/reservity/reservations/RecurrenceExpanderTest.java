package dev.tmmc.reservity.reservations;

import dev.tmmc.reservity.reservations.service.RecurrenceExpander;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceExpanderTest {

    private final RecurrenceExpander expander = new RecurrenceExpander();

    @Test
    void expandWeekly_returns_12_occurrences() {
        Instant start = Instant.parse("2026-05-08T14:00:00Z");
        Instant end   = Instant.parse("2026-05-08T16:00:00Z");
        List<RecurrenceExpander.Window> windows = expander.expandWeekly(start, end);
        assertEquals(12, windows.size());
        assertEquals(12, RecurrenceExpander.WEEKLY_OCCURRENCES);
    }

    @Test
    void expandWeekly_first_occurrence_is_seed() {
        Instant start = Instant.parse("2026-05-08T14:00:00Z");
        Instant end   = Instant.parse("2026-05-08T16:00:00Z");
        var first = expander.expandWeekly(start, end).get(0);
        assertEquals(0, first.index());
        assertEquals(start, first.startsAt());
        assertEquals(end, first.endsAt());
    }

    @Test
    void each_subsequent_occurrence_is_seven_days_later() {
        Instant start = Instant.parse("2026-05-08T14:00:00Z");
        Instant end   = Instant.parse("2026-05-08T16:00:00Z");
        List<RecurrenceExpander.Window> windows = expander.expandWeekly(start, end);
        for (int i = 1; i < windows.size(); i++) {
            long days = ChronoUnit.DAYS.between(windows.get(i - 1).startsAt(), windows.get(i).startsAt());
            assertEquals(7L, days, "gap before occurrence " + i);
        }
    }

    @Test
    void last_occurrence_is_77_days_after_seed() {
        Instant start = Instant.parse("2026-05-08T14:00:00Z");
        Instant end   = Instant.parse("2026-05-08T16:00:00Z");
        var last = expander.expandWeekly(start, end).get(11);
        assertEquals(11, last.index());
        assertEquals(start.plus(77, ChronoUnit.DAYS), last.startsAt());
    }

    @Test
    void invalid_input_throws() {
        Instant t = Instant.parse("2026-05-08T14:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> expander.expandWeekly(null, t));
        assertThrows(IllegalArgumentException.class, () -> expander.expandWeekly(t, null));
        assertThrows(IllegalArgumentException.class, () -> expander.expandWeekly(t, t));
        assertThrows(IllegalArgumentException.class, () -> expander.expandWeekly(t.plusSeconds(1), t));
    }
}
