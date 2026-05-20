package dev.tmmc.reservity.spaces.availability;

import java.time.LocalDate;
import java.util.List;

public record DayAvailability(
        LocalDate date,
        String dayOfWeek,
        List<HourSlot> hours,
        List<EventDto> events,
        List<Integer> booked,
        boolean fullyBooked,
        boolean closedAllDay
) {

    public record EventDto(
            String source,
            java.util.UUID id,
            int startH,
            int endH,
            String title,
            String host
    ) {
        public static EventDto from(EventBlock b) {
            return new EventDto(
                    b.source().name(),
                    b.id(),
                    b.startH(),
                    b.endH(),
                    b.title(),
                    b.host()
            );
        }
    }
}
