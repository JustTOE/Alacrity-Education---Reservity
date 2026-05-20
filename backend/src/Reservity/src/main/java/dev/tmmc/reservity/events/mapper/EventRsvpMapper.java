package dev.tmmc.reservity.events.mapper;

import dev.tmmc.reservity.events.dto.EventAttendeeSummary;
import dev.tmmc.reservity.events.dto.EventRsvpResponse;
import dev.tmmc.reservity.events.entity.EventRsvp;
import dev.tmmc.reservity.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class EventRsvpMapper {

    public EventRsvpResponse toResponse(EventRsvp rsvp) {
        User u = rsvp.getUser();
        return new EventRsvpResponse(
                rsvp.getEvent().getId(),
                u.getId(),
                u.getDisplayName(),
                u.getHandle(),
                rsvp.getStatus(),
                rsvp.getCreatedAt(),
                rsvp.getUpdatedAt());
    }

    public EventAttendeeSummary toAttendeeSummary(EventRsvp rsvp) {
        User u = rsvp.getUser();
        return new EventAttendeeSummary(
                u.getId(),
                u.getHandle(),
                u.getDisplayName(),
                rsvp.getStatus(),
                rsvp.getCreatedAt());
    }
}
