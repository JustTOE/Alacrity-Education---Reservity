package dev.tmmc.reservity.events.dto;

import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /api/events/{id}/rsvp}. Server validates the requested
 * status (only GOING / INTERESTED / CANT_GO accepted from clients; CANCELLED
 * comes via DELETE; WAITLIST is server-assigned).
 */
public record RsvpRequest(
        @NotNull RsvpStatus status
) {}
