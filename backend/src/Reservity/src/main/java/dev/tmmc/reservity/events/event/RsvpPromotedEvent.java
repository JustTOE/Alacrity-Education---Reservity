package dev.tmmc.reservity.events.event;

import java.util.UUID;

/**
 * Published when a WAITLIST RSVP is auto-promoted to GOING after a slot
 * opens up. Listener notifies the promoted user with a "you're in" toast.
 */
public record RsvpPromotedEvent(
        UUID eventId,
        UUID promotedUserId,
        String eventTitle
) {}
