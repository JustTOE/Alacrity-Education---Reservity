package dev.tmmc.reservity.events.service;

import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Pure function: {@code (now, startsAt, endsAt) → EventDisplayStatus}.
 *
 * <p>Boundary semantics:
 * <ul>
 *   <li>{@code LIVE}  if {@code now ∈ [startsAt, endsAt)}</li>
 *   <li>{@code SOON}  if {@code now < startsAt} and {@code startsAt - now ≤ 60 min}</li>
 *   <li>{@code TOMORROW} if {@code startsAt}'s UTC date == {@code now}'s UTC date + 1
 *       (and not already SOON or LIVE)</li>
 *   <li>{@code UPCOMING} otherwise (future)</li>
 *   <li>{@code PAST} if {@code now ≥ endsAt}</li>
 * </ul>
 *
 * <p>SOON wins over TOMORROW when both could match (a 23:00-tomorrow event
 * 60 minutes from now at 22:00 today is still SOON, not TOMORROW).
 */
@Component
public class EventDisplayStatusCalculator {

    private static final long SOON_WINDOW_MINUTES = 60;

    public EventDisplayStatus calculate(Instant now, Instant startsAt, Instant endsAt) {
        if (now == null || startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("now, startsAt, endsAt must all be non-null");
        }
        if (!now.isBefore(endsAt)) return EventDisplayStatus.PAST;
        if (!now.isBefore(startsAt)) return EventDisplayStatus.LIVE;

        long minutesUntil = Duration.between(now, startsAt).toMinutes();
        if (minutesUntil <= SOON_WINDOW_MINUTES) return EventDisplayStatus.SOON;

        LocalDate tomorrow = now.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1);
        LocalDate eventDate = startsAt.atZone(ZoneOffset.UTC).toLocalDate();
        if (eventDate.equals(tomorrow)) return EventDisplayStatus.TOMORROW;

        return EventDisplayStatus.UPCOMING;
    }
}
