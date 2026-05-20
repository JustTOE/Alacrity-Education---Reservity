package dev.tmmc.reservity.spaces.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Mirrors the frontend's SpaceType union: 'lab' | 'pod' | 'open' | 'studio'.
 * Stored in Postgres as lowercase strings (CHECK constraint enforces it),
 * so JPA conversion is delegated to {@link SpaceTypeConverter} and JSON
 * conversion uses {@link #value()} / {@link #fromValue(String)}.
 */
public enum SpaceType {
    LAB("lab"),
    POD("pod"),
    OPEN("open"),
    STUDIO("studio");

    private final String value;

    SpaceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SpaceType fromValue(String raw) {
        if (raw == null) return null;
        for (SpaceType t : values()) {
            if (t.value.equalsIgnoreCase(raw)) return t;
        }
        throw new IllegalArgumentException("Unknown SpaceType: " + raw);
    }
}
