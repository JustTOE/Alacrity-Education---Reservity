package dev.tmmc.reservity.events;

import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.service.EventDisplayStatusCalculator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventDisplayStatusCalculatorTest {

    private final EventDisplayStatusCalculator calc = new EventDisplayStatusCalculator();

    @Test
    void now_equals_startsAt_is_LIVE() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        assertEquals(EventDisplayStatus.LIVE,
                calc.calculate(now, now, now.plus(Duration.ofHours(1))));
    }

    @Test
    void now_equals_endsAt_is_PAST() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        assertEquals(EventDisplayStatus.PAST,
                calc.calculate(now, now.minus(Duration.ofHours(1)), now));
    }

    @Test
    void inside_window_is_LIVE() {
        Instant now = Instant.parse("2026-05-07T10:30:00Z");
        Instant start = Instant.parse("2026-05-07T10:00:00Z");
        Instant end = Instant.parse("2026-05-07T11:00:00Z");
        assertEquals(EventDisplayStatus.LIVE, calc.calculate(now, start, end));
    }

    @Test
    void thirty_minutes_out_is_SOON() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        Instant start = now.plus(Duration.ofMinutes(30));
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.SOON, calc.calculate(now, start, end));
    }

    @Test
    void sixty_minutes_out_is_SOON_inclusive() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        Instant start = now.plus(Duration.ofMinutes(60));
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.SOON, calc.calculate(now, start, end));
    }

    @Test
    void sixty_one_minutes_out_same_day_is_UPCOMING() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        Instant start = now.plus(Duration.ofMinutes(61));
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.UPCOMING, calc.calculate(now, start, end));
    }

    @Test
    void next_day_beyond_soon_window_is_TOMORROW() {
        Instant now = Instant.parse("2026-05-07T22:00:00Z");
        // start at 09:00 tomorrow UTC — well beyond the 60-min SOON window.
        Instant start = ZonedDateTime.of(2026, 5, 8, 9, 0, 0, 0, ZoneOffset.UTC)
                .toInstant();
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.TOMORROW, calc.calculate(now, start, end));
    }

    @Test
    void within_60_min_but_starts_tomorrow_is_SOON_not_TOMORROW() {
        // SOON wins over TOMORROW. now = 23:30 today UTC, start = 00:15 tomorrow.
        Instant now = ZonedDateTime.of(2026, 5, 7, 23, 30, 0, 0, ZoneOffset.UTC).toInstant();
        Instant start = ZonedDateTime.of(2026, 5, 8, 0, 15, 0, 0, ZoneOffset.UTC).toInstant();
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.SOON, calc.calculate(now, start, end));
    }

    @Test
    void five_days_out_is_UPCOMING() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        Instant start = now.plus(Duration.ofDays(5));
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.UPCOMING, calc.calculate(now, start, end));
    }

    @Test
    void two_days_out_is_UPCOMING_not_TOMORROW() {
        Instant now = Instant.parse("2026-05-07T10:00:00Z");
        Instant start = ZonedDateTime.of(2026, 5, 9, 14, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant end = start.plus(Duration.ofHours(1));
        assertEquals(EventDisplayStatus.UPCOMING, calc.calculate(now, start, end));
    }

    @Test
    void null_input_throws() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> calc.calculate(null, now, now));
        assertThrows(IllegalArgumentException.class,
                () -> calc.calculate(now, null, now));
        assertThrows(IllegalArgumentException.class,
                () -> calc.calculate(now, now, null));
    }
}
