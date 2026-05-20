package dev.tmmc.reservity.spaces.availability;

import java.time.LocalTime;

public record DayWindow(LocalTime open, LocalTime close) {

    public DayWindow {
        if (open == null || close == null) {
            throw new IllegalArgumentException("DayWindow open/close must not be null");
        }
        if (!close.isAfter(open)) {
            throw new IllegalArgumentException("DayWindow close (" + close + ") must be after open (" + open + ")");
        }
    }

    public static DayWindow allDay() {
        return new DayWindow(LocalTime.MIDNIGHT, LocalTime.of(23, 59, 59));
    }
}
