package dev.tmmc.reservity.spaces.availability;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code operating_hours} JSONB column. Two modes:
 * <ul>
 *   <li>{@code {"mode":"24/7"}} — always open</li>
 *   <li>{@code {"mode":"weekly", "schedule":{ "MON":[{"open":"08:00","close":"22:00"}], ... }, "exceptions":[ {"date":"2026-12-25","closed":true}, {"date":"...","open":"...","close":"..."} ] }}</li>
 * </ul>
 * Falls back to {@link OperatingHours.AlwaysOpen} on any malformed input (warning logged) so a single
 * bad row doesn't take an availability query down.
 */
@Component
@Slf4j
public class OperatingHoursParser {

    private static final Map<String, DayOfWeek> DAY_KEYS = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY
    );

    public OperatingHours parse(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return new OperatingHours.AlwaysOpen();
        }
        String mode = textOrEmpty(node, "mode");
        if (mode.isEmpty() || mode.equalsIgnoreCase("24/7")) {
            return new OperatingHours.AlwaysOpen();
        }
        if (mode.equalsIgnoreCase("weekly")) {
            try {
                return parseWeekly(node);
            } catch (RuntimeException ex) {
                log.warn("Falling back to AlwaysOpen — failed to parse weekly operating_hours: {}", ex.getMessage());
                return new OperatingHours.AlwaysOpen();
            }
        }
        log.warn("Falling back to AlwaysOpen — unknown operating_hours mode '{}'", mode);
        return new OperatingHours.AlwaysOpen();
    }

    private OperatingHours.Weekly parseWeekly(JsonNode node) {
        Map<DayOfWeek, List<DayWindow>> schedule = new EnumMap<>(DayOfWeek.class);
        JsonNode scheduleNode = node.get("schedule");
        if (scheduleNode != null && scheduleNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = scheduleNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                DayOfWeek day = DAY_KEYS.get(entry.getKey().toUpperCase());
                if (day == null) {
                    log.warn("Skipping unknown weekday key '{}' in operating_hours.schedule", entry.getKey());
                    continue;
                }
                schedule.put(day, parseWindowArray(entry.getValue()));
            }
        }

        Map<LocalDate, List<DayWindow>> exceptions = new HashMap<>();
        JsonNode exceptionsNode = node.get("exceptions");
        if (exceptionsNode != null && exceptionsNode.isArray()) {
            for (JsonNode ex : exceptionsNode) {
                if (!ex.isObject() || !ex.hasNonNull("date")) continue;
                LocalDate date;
                try {
                    date = LocalDate.parse(ex.get("date").asText());
                } catch (DateTimeParseException dtpe) {
                    log.warn("Skipping operating_hours exception with invalid date '{}'", ex.get("date").asText());
                    continue;
                }
                if (ex.path("closed").asBoolean(false)) {
                    exceptions.put(date, List.of());
                } else if (ex.hasNonNull("open") && ex.hasNonNull("close")) {
                    LocalTime open = LocalTime.parse(ex.get("open").asText());
                    LocalTime close = LocalTime.parse(ex.get("close").asText());
                    exceptions.put(date, List.of(new DayWindow(open, close)));
                }
            }
        }
        return new OperatingHours.Weekly(schedule, exceptions);
    }

    private List<DayWindow> parseWindowArray(JsonNode arr) {
        if (arr == null || !arr.isArray()) return List.of();
        List<DayWindow> windows = new ArrayList<>(arr.size());
        for (JsonNode win : arr) {
            if (!win.isObject() || !win.hasNonNull("open") || !win.hasNonNull("close")) continue;
            try {
                LocalTime open = LocalTime.parse(win.get("open").asText());
                LocalTime close = LocalTime.parse(win.get("close").asText());
                windows.add(new DayWindow(open, close));
            } catch (DateTimeParseException | IllegalArgumentException ex) {
                log.warn("Skipping malformed operating_hours window: {}", win);
            }
        }
        return windows;
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? "" : child.asText("");
    }
}
