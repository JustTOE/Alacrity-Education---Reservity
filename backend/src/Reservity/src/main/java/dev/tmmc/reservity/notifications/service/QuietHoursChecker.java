package dev.tmmc.reservity.notifications.service;

import dev.tmmc.reservity.user.entity.UserSettings;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Pure utility — given a user's settings and the current instant, decide
 * whether email delivery should be suppressed because we're inside their
 * quiet-hours window. Boundary semantics: {@code [start, end)} — start is
 * inclusive, end is exclusive. Cross-midnight windows (start &gt; end) wrap
 * naturally as {@code [start, 24:00) ∪ [00:00, end)}.
 *
 * <p>Returns {@code false} (= "not in quiet hours") whenever the inputs
 * leave anything ambiguous: null settings, null start, null end, equal
 * boundaries (zero-width window means "off"). The in-app notification path
 * never consults this — only emails are suppressed.
 */
@Component
public class QuietHoursChecker {

    public boolean inQuietHours(UserSettings settings, Instant now) {
        if (settings == null
                || settings.getQuietHoursStart() == null
                || settings.getQuietHoursEnd() == null) {
            return false;
        }
        ZoneId zone = ZoneId.of(settings.getTimezone() == null ? "UTC" : settings.getTimezone());
        LocalTime nowLocal = now.atZone(zone).toLocalTime();
        LocalTime start = settings.getQuietHoursStart();
        LocalTime end = settings.getQuietHoursEnd();
        if (start.equals(end)) return false;
        if (start.isBefore(end)) {
            return !nowLocal.isBefore(start) && nowLocal.isBefore(end);
        }
        // Wraps midnight (e.g., 22:00–07:00):
        return !nowLocal.isBefore(start) || nowLocal.isBefore(end);
    }
}
