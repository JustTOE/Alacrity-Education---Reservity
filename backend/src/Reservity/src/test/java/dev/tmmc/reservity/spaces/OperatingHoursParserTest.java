package dev.tmmc.reservity.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tmmc.reservity.spaces.availability.DayWindow;
import dev.tmmc.reservity.spaces.availability.OperatingHours;
import dev.tmmc.reservity.spaces.availability.OperatingHoursParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit test — no Spring context. Exercises every branch of
 * {@link OperatingHoursParser} so the rest of the M4 stack can trust its
 * output even on malformed JSONB rows.
 */
class OperatingHoursParserTest {

    private final ObjectMapper json = new ObjectMapper();
    private final OperatingHoursParser parser = new OperatingHoursParser();

    @Test
    void mode247_returns_AlwaysOpen_with_full_day_window() throws Exception {
        JsonNode node = json.readTree("{\"mode\":\"24/7\"}");
        OperatingHours hours = parser.parse(node);
        assertInstanceOf(OperatingHours.AlwaysOpen.class, hours);
        List<DayWindow> windows = hours.windowsFor(LocalDate.of(2026, 5, 7));
        assertEquals(1, windows.size());
        assertEquals(LocalTime.MIDNIGHT, windows.get(0).open());
    }

    @Test
    void weekly_returns_per_day_windows_and_empty_for_unspecified_days() throws Exception {
        JsonNode node = json.readTree("""
            {
              "mode":"weekly",
              "schedule":{
                "MON":[{"open":"08:00","close":"22:00"}],
                "TUE":[{"open":"08:00","close":"22:00"}],
                "WED":[{"open":"08:00","close":"22:00"}],
                "THU":[{"open":"08:00","close":"22:00"}],
                "FRI":[{"open":"08:00","close":"22:00"}]
              }
            }
            """);
        OperatingHours hours = parser.parse(node);
        assertInstanceOf(OperatingHours.Weekly.class, hours);

        // 2026-05-04 is a Monday.
        List<DayWindow> mon = hours.windowsFor(LocalDate.of(2026, 5, 4));
        assertEquals(1, mon.size());
        assertEquals(LocalTime.of(8, 0), mon.get(0).open());
        assertEquals(LocalTime.of(22, 0), mon.get(0).close());

        // 2026-05-09 is Saturday — no entry → empty.
        List<DayWindow> sat = hours.windowsFor(LocalDate.of(2026, 5, 9));
        assertTrue(sat.isEmpty());
    }

    @Test
    void weekly_exception_can_close_a_day_explicitly() throws Exception {
        JsonNode node = json.readTree("""
            {
              "mode":"weekly",
              "schedule":{ "FRI":[{"open":"09:00","close":"17:00"}] },
              "exceptions":[
                {"date":"2026-12-25","closed":true}
              ]
            }
            """);
        OperatingHours hours = parser.parse(node);
        // 2026-12-25 is a Friday, but the exception explicitly closes it.
        assertTrue(hours.windowsFor(LocalDate.of(2026, 12, 25)).isEmpty());
    }

    @Test
    void weekly_exception_can_override_with_a_custom_window() throws Exception {
        JsonNode node = json.readTree("""
            {
              "mode":"weekly",
              "schedule":{ "THU":[{"open":"08:00","close":"22:00"}] },
              "exceptions":[
                {"date":"2026-12-31","open":"08:00","close":"16:00"}
              ]
            }
            """);
        OperatingHours hours = parser.parse(node);
        List<DayWindow> windows = hours.windowsFor(LocalDate.of(2026, 12, 31));
        assertEquals(1, windows.size());
        assertEquals(LocalTime.of(16, 0), windows.get(0).close());
    }

    @Test
    void multiple_windows_per_day_are_all_returned() throws Exception {
        JsonNode node = json.readTree("""
            {
              "mode":"weekly",
              "schedule":{ "MON":[
                {"open":"08:00","close":"12:00"},
                {"open":"14:00","close":"18:00"}
              ]}
            }
            """);
        OperatingHours hours = parser.parse(node);
        List<DayWindow> mon = hours.windowsFor(LocalDate.of(2026, 5, 4));
        assertEquals(2, mon.size());
        assertEquals(LocalTime.of(12, 0), mon.get(0).close());
        assertEquals(LocalTime.of(14, 0), mon.get(1).open());
    }

    @Test
    void malformed_input_falls_back_to_AlwaysOpen() throws Exception {
        // Garbage shape — parser should warn-log and degrade gracefully.
        JsonNode node = json.readTree("\"not-an-object\"");
        assertInstanceOf(OperatingHours.AlwaysOpen.class, parser.parse(node));

        JsonNode unknownMode = json.readTree("{\"mode\":\"hourly\"}");
        assertInstanceOf(OperatingHours.AlwaysOpen.class, parser.parse(unknownMode));

        assertInstanceOf(OperatingHours.AlwaysOpen.class, parser.parse(null));
    }

    @Test
    void weekly_with_invalid_window_skips_only_the_bad_window() throws Exception {
        JsonNode node = json.readTree("""
            {
              "mode":"weekly",
              "schedule":{ "MON":[
                {"open":"not-a-time","close":"22:00"},
                {"open":"08:00","close":"22:00"}
              ]}
            }
            """);
        OperatingHours hours = parser.parse(node);
        List<DayWindow> mon = hours.windowsFor(LocalDate.of(2026, 5, 4));
        assertEquals(1, mon.size(), "the malformed window should be dropped, the valid one kept");
    }
}
