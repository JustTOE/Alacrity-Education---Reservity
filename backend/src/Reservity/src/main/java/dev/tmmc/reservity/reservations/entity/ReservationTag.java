package dev.tmmc.reservity.reservations.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Mirrors the frontend's {@code HistoryEntry.tag} union. Stored as the
 * display string (CHECK constraint at DB level matches verbatim), so the
 * round-trip uses {@link ReservationTagConverter}. Default for new requests
 * is {@link #SOLO_FOCUS} when the client omits the field.
 */
public enum ReservationTag {
    SOLO_FOCUS("Solo focus"),
    GROUP_SESSION("Group session"),
    DROP_IN("Drop-in"),
    EVENT("Event"),
    OTHER("Other");

    private final String value;

    ReservationTag(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ReservationTag fromValue(String raw) {
        if (raw == null) return null;
        for (ReservationTag t : values()) {
            if (t.value.equalsIgnoreCase(raw)) return t;
        }
        throw new IllegalArgumentException("Unknown ReservationTag: " + raw);
    }
}
