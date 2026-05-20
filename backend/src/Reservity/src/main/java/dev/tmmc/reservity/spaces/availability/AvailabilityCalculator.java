package dev.tmmc.reservity.spaces.availability;

import dev.tmmc.reservity.spaces.entity.SpaceClosure;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure (no IO) function: given a space's operating hours, its closures, any
 * pre-resolved event/reservation blocks, and a date, return a 24-hour
 * {@link DayAvailability} the API can serialise as-is.
 *
 * <p>Layer precedence (later layers override earlier):
 * <ol>
 *   <li>Default: every hour is {@link SlotState#CLOSED}.</li>
 *   <li>Operating-hours windows flip covered hours to {@link SlotState#OPEN}.</li>
 *   <li>Closures flip back to {@link SlotState#CLOSED}.</li>
 *   <li>Reservations flip to {@link SlotState#RESERVED}.</li>
 *   <li>Events flip to {@link SlotState#EVENT}.</li>
 * </ol>
 *
 * <p>The frontend's existing {@code booked: number[]} contract is preserved by
 * collecting all hour indices whose final state is {@code RESERVED} or
 * {@code EVENT}.
 */
@Component
public class AvailabilityCalculator {

    public static final int HOURS_PER_DAY = 24;

    public DayAvailability calculate(LocalDate date,
                                     ZoneId zone,
                                     OperatingHours hours,
                                     List<SpaceClosure> closures,
                                     List<EventBlock> events) {
        SlotState[] states = new SlotState[HOURS_PER_DAY];
        java.util.Arrays.fill(states, SlotState.CLOSED);
        String[] labels = new String[HOURS_PER_DAY];
        java.util.UUID[] eventIds = new java.util.UUID[HOURS_PER_DAY];
        java.util.UUID[] reservationIds = new java.util.UUID[HOURS_PER_DAY];

        // 2. operating hours → OPEN
        for (DayWindow w : hours.windowsFor(date)) {
            int from = w.open().getHour();
            int to = w.close().equals(LocalTime.MIDNIGHT) ? HOURS_PER_DAY : ceilHour(w.close());
            for (int h = from; h < to && h < HOURS_PER_DAY; h++) {
                states[h] = SlotState.OPEN;
            }
        }

        // 3. closures → CLOSED (subtract from operating hours)
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        if (closures != null) {
            for (SpaceClosure c : closures) {
                Instant cs = c.getStartsAt().isBefore(dayStart) ? dayStart : c.getStartsAt();
                Instant ce = c.getEndsAt().isAfter(dayEnd) ? dayEnd : c.getEndsAt();
                if (!ce.isAfter(cs)) continue;
                int from = ZonedDateTime.ofInstant(cs, zone).getHour();
                int to = ce.equals(dayEnd) ? HOURS_PER_DAY
                        : ceilHourOfDay(ZonedDateTime.ofInstant(ce, zone));
                for (int h = from; h < to && h < HOURS_PER_DAY; h++) {
                    states[h] = SlotState.CLOSED;
                    labels[h] = null;
                }
            }
        }

        // 4 + 5. reservations & events
        if (events != null) {
            for (EventBlock e : events) {
                int from = Math.max(0, e.startH());
                int to = Math.min(HOURS_PER_DAY, e.endH());
                for (int h = from; h < to; h++) {
                    if (e.source() == EventBlock.Source.RESERVATION) {
                        states[h] = SlotState.RESERVED;
                        reservationIds[h] = e.id();
                    } else {
                        states[h] = SlotState.EVENT;
                        eventIds[h] = e.id();
                    }
                    labels[h] = e.title();
                }
            }
        }

        List<HourSlot> hourSlots = new ArrayList<>(HOURS_PER_DAY);
        List<Integer> booked = new ArrayList<>();
        int openCount = 0;
        int activeCount = 0;
        for (int h = 0; h < HOURS_PER_DAY; h++) {
            hourSlots.add(new HourSlot(h, states[h], labels[h], eventIds[h], reservationIds[h]));
            if (states[h] == SlotState.OPEN) openCount++;
            if (states[h] != SlotState.CLOSED) activeCount++;
            if (states[h] == SlotState.RESERVED || states[h] == SlotState.EVENT) {
                booked.add(h);
            }
        }

        boolean closedAllDay = (activeCount == 0);
        boolean fullyBooked = !closedAllDay && (openCount == 0);

        List<DayAvailability.EventDto> dtoEvents = new ArrayList<>();
        if (events != null) {
            for (EventBlock e : events) dtoEvents.add(DayAvailability.EventDto.from(e));
        }

        return new DayAvailability(
                date,
                date.getDayOfWeek().name(),
                Collections.unmodifiableList(hourSlots),
                Collections.unmodifiableList(dtoEvents),
                Collections.unmodifiableList(booked),
                fullyBooked,
                closedAllDay
        );
    }

    /** A close-time of 17:30 should mark hour 17 as part of the window (round up). */
    private static int ceilHour(LocalTime t) {
        if (t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0) {
            return t.getHour();
        }
        return Math.min(HOURS_PER_DAY, t.getHour() + 1);
    }

    private static int ceilHourOfDay(ZonedDateTime zdt) {
        int hour = zdt.getHour();
        if (zdt.getMinute() == 0 && zdt.getSecond() == 0 && zdt.getNano() == 0) {
            return hour;
        }
        return Math.min(HOURS_PER_DAY, hour + 1);
    }
}
