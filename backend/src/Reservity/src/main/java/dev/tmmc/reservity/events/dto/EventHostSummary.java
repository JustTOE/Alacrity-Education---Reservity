package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.EventHostType;

import java.util.UUID;

/**
 * Polymorphic host pointer for response payloads. {@code displayName} +
 * {@code initials} are pre-resolved by the mapper so the FE doesn't make a
 * second round-trip.
 */
public record EventHostSummary(
        EventHostType type,
        UUID id,
        String displayName,
        String initials,
        String handleOrSlug
) {}
