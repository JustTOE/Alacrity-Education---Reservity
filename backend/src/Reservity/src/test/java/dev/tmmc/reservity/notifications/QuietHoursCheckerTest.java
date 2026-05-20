package dev.tmmc.reservity.notifications;

import dev.tmmc.reservity.notifications.service.QuietHoursChecker;
import dev.tmmc.reservity.user.entity.UserSettings;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit, no Spring. Boundary semantics:
 * <ul>
 *   <li>Start is inclusive, end is exclusive ({@code [start, end)}).</li>
 *   <li>Cross-midnight wrap (start &gt; end) is supported.</li>
 *   <li>Null start/end or zero-width window means "off" → {@code false}.</li>
 * </ul>
 */
class QuietHoursCheckerTest {

    private final QuietHoursChecker checker = new QuietHoursChecker();

    @Test
    void null_settings_returns_false() {
        assertFalse(checker.inQuietHours(null, Instant.now()));
    }

    @Test
    void null_start_or_end_returns_false() {
        UserSettings s = utcSettings(null, LocalTime.of(7, 0));
        assertFalse(checker.inQuietHours(s, Instant.now()));
        UserSettings s2 = utcSettings(LocalTime.of(22, 0), null);
        assertFalse(checker.inQuietHours(s2, Instant.now()));
    }

    @Test
    void same_day_window_inside_returns_true() {
        UserSettings s = utcSettings(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(checker.inQuietHours(s, atUtc(10, 0)));
    }

    @Test
    void same_day_window_before_start_returns_false_open_boundary() {
        UserSettings s = utcSettings(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertFalse(checker.inQuietHours(s, atUtc(8, 59)));
    }

    @Test
    void same_day_window_at_end_returns_false_close_exclusive() {
        UserSettings s = utcSettings(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertFalse(checker.inQuietHours(s, atUtc(17, 0)));
    }

    @Test
    void same_day_window_at_start_returns_true_inclusive() {
        UserSettings s = utcSettings(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(checker.inQuietHours(s, atUtc(9, 0)));
    }

    @Test
    void cross_midnight_window_in_first_half_returns_true() {
        UserSettings s = utcSettings(LocalTime.of(22, 0), LocalTime.of(7, 0));
        assertTrue(checker.inQuietHours(s, atUtc(23, 30)));
    }

    @Test
    void cross_midnight_window_in_second_half_returns_true() {
        UserSettings s = utcSettings(LocalTime.of(22, 0), LocalTime.of(7, 0));
        assertTrue(checker.inQuietHours(s, atUtc(2, 0)));
    }

    @Test
    void cross_midnight_window_outside_returns_false() {
        UserSettings s = utcSettings(LocalTime.of(22, 0), LocalTime.of(7, 0));
        assertFalse(checker.inQuietHours(s, atUtc(12, 0)));
    }

    @Test
    void cross_midnight_at_end_returns_false_close_exclusive() {
        UserSettings s = utcSettings(LocalTime.of(22, 0), LocalTime.of(7, 0));
        assertFalse(checker.inQuietHours(s, atUtc(7, 0)));
    }

    @Test
    void cross_midnight_at_start_returns_true_inclusive() {
        UserSettings s = utcSettings(LocalTime.of(22, 0), LocalTime.of(7, 0));
        assertTrue(checker.inQuietHours(s, atUtc(22, 0)));
    }

    @Test
    void zero_width_window_returns_false() {
        UserSettings s = utcSettings(LocalTime.of(12, 0), LocalTime.of(12, 0));
        assertFalse(checker.inQuietHours(s, atUtc(12, 0)));
    }

    @Test
    void non_utc_timezone_resolves_correctly() {
        // Romania DST-aware: Europe/Bucharest is UTC+2 (winter) / UTC+3 (summer).
        // Pick a winter date so DST doesn't surprise the test.
        UserSettings s = UserSettings.builder()
                .quietHoursStart(LocalTime.of(23, 0))
                .quietHoursEnd(LocalTime.of(7, 0))
                .timezone("Europe/Bucharest")
                .locale("en")
                .theme(dev.tmmc.reservity.user.entity.Theme.auto)
                .accent(dev.tmmc.reservity.user.entity.Accent.indigo)
                .density(dev.tmmc.reservity.user.entity.Density.comfortable)
                .build();
        // 2026-01-15 22:00 UTC = 00:00 EET (winter, UTC+2). That falls inside 23:00–07:00.
        Instant winterMidnightLocal = LocalDateTime.of(LocalDate.of(2026, 1, 15), LocalTime.of(22, 0))
                .toInstant(ZoneOffset.UTC);
        assertTrue(checker.inQuietHours(s, winterMidnightLocal));

        // 2026-01-15 10:00 UTC = 12:00 EET → outside.
        Instant winterNoonLocal = LocalDateTime.of(LocalDate.of(2026, 1, 15), LocalTime.of(10, 0))
                .toInstant(ZoneOffset.UTC);
        assertFalse(checker.inQuietHours(s, winterNoonLocal));
    }

    @Test
    void null_timezone_falls_back_to_utc() {
        UserSettings s = UserSettings.builder()
                .quietHoursStart(LocalTime.of(22, 0))
                .quietHoursEnd(LocalTime.of(7, 0))
                .timezone(null)
                .locale("en")
                .theme(dev.tmmc.reservity.user.entity.Theme.auto)
                .accent(dev.tmmc.reservity.user.entity.Accent.indigo)
                .density(dev.tmmc.reservity.user.entity.Density.comfortable)
                .build();
        assertTrue(checker.inQuietHours(s, atUtc(2, 0)));
    }

    // ────────── helpers ──────────

    private static UserSettings utcSettings(LocalTime start, LocalTime end) {
        return UserSettings.builder()
                .quietHoursStart(start)
                .quietHoursEnd(end)
                .timezone("UTC")
                .locale("en")
                .theme(dev.tmmc.reservity.user.entity.Theme.auto)
                .accent(dev.tmmc.reservity.user.entity.Accent.indigo)
                .density(dev.tmmc.reservity.user.entity.Density.comfortable)
                .build();
    }

    private static Instant atUtc(int hour, int minute) {
        return LocalDateTime.of(LocalDate.of(2026, 5, 8), LocalTime.of(hour, minute))
                .toInstant(ZoneOffset.UTC);
    }
}
