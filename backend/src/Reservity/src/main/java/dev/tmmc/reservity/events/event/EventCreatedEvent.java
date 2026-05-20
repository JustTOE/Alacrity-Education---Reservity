package dev.tmmc.reservity.events.event;

import java.util.UUID;

/**
 * Published after an event row is committed. Reserved for future
 * notification fan-out (e.g., notify everyone following the host). M8 has
 * no listener — the record exists so the listener can be added later
 * without changing the service signature.
 */
public record EventCreatedEvent(
        UUID eventId
) {}
