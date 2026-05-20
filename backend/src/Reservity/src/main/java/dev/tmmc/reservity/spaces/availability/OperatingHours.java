package dev.tmmc.reservity.spaces.availability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public sealed interface OperatingHours permits OperatingHours.AlwaysOpen, OperatingHours.Weekly {

    List<DayWindow> windowsFor(LocalDate date);

    record AlwaysOpen() implements OperatingHours {
        @Override
        public List<DayWindow> windowsFor(LocalDate date) {
            return List.of(DayWindow.allDay());
        }
    }

    record Weekly(Map<DayOfWeek, List<DayWindow>> schedule,
                  Map<LocalDate, List<DayWindow>> exceptions) implements OperatingHours {

        public Weekly {
            schedule = schedule == null ? Map.of() : Map.copyOf(schedule);
            exceptions = exceptions == null ? Map.of() : Map.copyOf(exceptions);
        }

        @Override
        public List<DayWindow> windowsFor(LocalDate date) {
            if (exceptions.containsKey(date)) {
                return exceptions.get(date);
            }
            return schedule.getOrDefault(date.getDayOfWeek(), List.of());
        }
    }
}
