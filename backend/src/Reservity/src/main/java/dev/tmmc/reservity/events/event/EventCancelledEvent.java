package dev.tmmc.reservity.events.event;

import java.util.List;
import java.util.UUID;

/**
 * Published when an event is cancelled. {@code notifyUserIds} is the snapshot
 * of all non-CANCELLED RSVPs at the moment of cancel — captured by the
 * service before publishing so the listener doesn't have to re-query. The
 * listener fans out one in-app notification per id.
 */
public record EventCancelledEvent(
        UUID eventId,
        List<UUID> notifyUserIds
) {}
