package dev.tmmc.reservity.spaces;

import dev.tmmc.reservity.spaces.availability.*;
import dev.tmmc.reservity.spaces.entity.SpaceClosure;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit test — no Spring context, no DB. Verifies the layering rules
 * documented on {@link AvailabilityCalculator}: CLOSED → OPEN → CLOSED →
 * RESERVED → EVENT.
 */
class AvailabilityCalculatorTest {

    private final AvailabilityCalculator calc = new AvailabilityCalculator();
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final LocalDate DATE = LocalDate.of(2026, 5, 7);

    @Test
    void alwaysOpen_no_closures_no_events_yields_24_open_slots() {
        DayAvailability day = calc.calculate(DATE, UTC, new OperatingHours.AlwaysOpen(), List.of(), List.of());
        assertEquals(24, day.hours().size());
        for (HourSlot slot : day.hours()) {
            assertEquals(SlotState.OPEN, slot.state());
        }
        assertFalse(day.fullyBooked());
        assertFalse(day.closedAllDay());
        assertTrue(day.booked().isEmpty());
    }

    @Test
    void alwaysOpen_full_day_closure_yields_all_closed() {
        SpaceClosure closure = SpaceClosure.builder()
                .startsAt(DATE.atStartOfDay(UTC).toInstant())
                .endsAt(DATE.plusDays(1).atStartOfDay(UTC).toInstant())
                .reason("Maintenance")
                .build();
        DayAvailability day = calc.calculate(DATE, UTC, new OperatingHours.AlwaysOpen(), List.of(closure), List.of());
        assertTrue(day.closedAllDay());
        for (HourSlot slot : day.hours()) {
            assertEquals(SlotState.CLOSED, slot.state());
        }
    }

    @Test
    void weekly_window_marks_only_those_hours_open() {
        OperatingHours.Weekly hours = new OperatingHours.Weekly(
                Map.of(DayOfWeek.THURSDAY, List.of(new DayWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)))),
                Map.of()
        );
        DayAvailability day = calc.calculate(DATE, UTC, hours, List.of(), List.of()); // 2026-05-07 is Thu
        for (int h = 0; h < 24; h++) {
            SlotState expected = (h >= 9 && h < 17) ? SlotState.OPEN : SlotState.CLOSED;
            assertEquals(expected, day.hours().get(h).state(), "hour " + h);
        }
        assertFalse(day.closedAllDay());
        assertFalse(day.fullyBooked());
    }

    @Test
    void closure_within_open_hours_overrides_to_closed() {
        OperatingHours.Weekly hours = new OperatingHours.Weekly(
                Map.of(DayOfWeek.THURSDAY, List.of(new DayWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)))),
                Map.of()
        );
        SpaceClosure closure = SpaceClosure.builder()
                .startsAt(DATE.atTime(14, 0).atZone(UTC).toInstant())
                .endsAt(DATE.atTime(15, 30).atZone(UTC).toInstant())
                .reason("Maintenance")
                .build();
        DayAvailability day = calc.calculate(DATE, UTC, hours, List.of(closure), List.of());
        // 14 fully covered → CLOSED; 15:00–15:30 partial → ceil-up to hour 16, so 15 also CLOSED.
        assertEquals(SlotState.CLOSED, day.hours().get(14).state());
        assertEquals(SlotState.CLOSED, day.hours().get(15).state());
        assertEquals(SlotState.OPEN, day.hours().get(13).state());
        assertEquals(SlotState.OPEN, day.hours().get(16).state());
    }

    @Test
    void event_overlay_marks_event_state_and_populates_booked_array() {
        EventBlock evt = new EventBlock(
                EventBlock.Source.EVENT,
                UUID.randomUUID(),
                10, 12,
                "Open lab",
                "TA office"
        );
        DayAvailability day = calc.calculate(DATE, UTC, new OperatingHours.AlwaysOpen(), List.of(), List.of(evt));
        assertEquals(SlotState.EVENT, day.hours().get(10).state());
        assertEquals("Open lab", day.hours().get(10).label());
        assertEquals(SlotState.EVENT, day.hours().get(11).state());
        assertEquals(SlotState.OPEN, day.hours().get(12).state());

        assertEquals(List.of(10, 11), day.booked());
        assertEquals(1, day.events().size());
        assertEquals("EVENT", day.events().get(0).source());
    }

    @Test
    void reservation_overlay_marks_reserved_state() {
        EventBlock res = new EventBlock(
                EventBlock.Source.RESERVATION,
                UUID.randomUUID(),
                14, 16,
                "Group session",
                "Maya R."
        );
        DayAvailability day = calc.calculate(DATE, UTC, new OperatingHours.AlwaysOpen(), List.of(), List.of(res));
        assertEquals(SlotState.RESERVED, day.hours().get(14).state());
        assertEquals(SlotState.RESERVED, day.hours().get(15).state());
        assertEquals(res.id(), day.hours().get(14).reservationId());
    }

    @Test
    void closure_spanning_midnight_is_clipped_per_day() {
        Instant start = DATE.atTime(23, 0).atZone(UTC).toInstant();
        Instant end = DATE.plusDays(1).atTime(2, 0).atZone(UTC).toInstant();
        SpaceClosure closure = SpaceClosure.builder().startsAt(start).endsAt(end).build();

        DayAvailability today = calc.calculate(DATE, UTC, new OperatingHours.AlwaysOpen(), List.of(closure), List.of());
        assertEquals(SlotState.CLOSED, today.hours().get(23).state());
        assertEquals(SlotState.OPEN, today.hours().get(22).state());

        DayAvailability tomorrow = calc.calculate(DATE.plusDays(1), UTC, new OperatingHours.AlwaysOpen(), List.of(closure), List.of());
        assertEquals(SlotState.CLOSED, tomorrow.hours().get(0).state());
        assertEquals(SlotState.CLOSED, tomorrow.hours().get(1).state());
        assertEquals(SlotState.OPEN, tomorrow.hours().get(2).state());
    }

    @Test
    void operating_hours_with_no_open_slots_marks_closedAllDay() {
        OperatingHours.Weekly hours = new OperatingHours.Weekly(
                Map.of(DayOfWeek.MONDAY, List.of(new DayWindow(LocalTime.of(9, 0), LocalTime.of(17, 0)))),
                Map.of()
        );
        // 2026-05-09 is a Saturday — no schedule entry.
        DayAvailability day = calc.calculate(LocalDate.of(2026, 5, 9), UTC, hours, List.of(), List.of());
        assertTrue(day.closedAllDay());
        for (HourSlot slot : day.hours()) {
            assertEquals(SlotState.CLOSED, slot.state());
        }
    }
}
