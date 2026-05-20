package dev.tmmc.reservity.events.mapper;

import dev.tmmc.reservity.events.dto.EventHostSummary;
import dev.tmmc.reservity.events.dto.EventLocation;
import dev.tmmc.reservity.events.dto.EventResponse;
import dev.tmmc.reservity.events.dto.EventSummary;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.service.EventDisplayStatusCalculator;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Hand-written entity → DTO mappers for the events package.
 *
 * <p>Resolves polymorphic host (USER / ORGANIZATION) by reading the actor
 * lazily from the appropriate repository — the mapper is invoked inside a
 * transactional service method so the lazy fetch is fine.
 */
@Component
@RequiredArgsConstructor
public class EventMapper {

    private final EventDisplayStatusCalculator displayStatusCalculator;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public EventResponse toResponse(Event event, RsvpStatus callerRsvp) {
        EventDisplayStatus display = displayStatusCalculator.calculate(
                Instant.now(), event.getStartsAt(), event.getEndsAt());
        return new EventResponse(
                event.getId(),
                event.getSlug(),
                resolveHost(event),
                resolveLocation(event),
                event.getReservation() == null ? null : event.getReservation().getId(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getTitle(),
                event.getBlurb(),
                event.getDescription(),
                event.getCoverImageUrl(),
                event.getCategory(),
                event.getTag(),
                event.getCapacity(),
                event.getAttendeesCount(),
                event.isRsvpRequired(),
                event.getVisibility(),
                event.getStatus(),
                display,
                callerRsvp,
                event.getCancelledAt(),
                event.getCancellationReason(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public EventSummary toSummary(Event event) {
        EventDisplayStatus display = displayStatusCalculator.calculate(
                Instant.now(), event.getStartsAt(), event.getEndsAt());
        EventHostSummary host = resolveHost(event);
        Space space = event.getSpace();
        Building building = event.getBuilding();
        return new EventSummary(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                event.getBlurb(),
                host == null ? null : host.displayName(),
                host == null ? null : host.initials(),
                space == null ? null : space.getName(),
                building == null ? null : building.getName(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCategory(),
                event.getTag(),
                event.getAttendeesCount(),
                event.getCapacity(),
                display,
                event.getStatus(),
                event.getCoverImageUrl());
    }

    private EventHostSummary resolveHost(Event event) {
        if (event.getHostType() == EventHostType.USER) {
            Optional<User> u = userRepository.findById(event.getHostId());
            return u.map(user -> new EventHostSummary(
                    EventHostType.USER,
                    user.getId(),
                    user.getDisplayName(),
                    user.getInitials(),
                    user.getHandle())).orElse(null);
        }
        Optional<Organization> o = organizationRepository.findById(event.getHostId());
        return o.map(org -> new EventHostSummary(
                EventHostType.ORGANIZATION,
                org.getId(),
                org.getShortName() == null ? org.getName() : org.getShortName(),
                deriveInitials(org.getName()),
                org.getSlug())).orElse(null);
    }

    private EventLocation resolveLocation(Event event) {
        Space space = event.getSpace();
        Building building = event.getBuilding();
        if (space == null && building == null && event.getLocationLabel() == null) return null;
        return new EventLocation(
                space == null ? null : space.getId(),
                space == null ? null : space.getSlug(),
                space == null ? null : space.getName(),
                building == null ? null : building.getId(),
                building == null ? null : building.getName(),
                event.getLocationLabel());
    }

    static String deriveInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 4; i++) {
            String p = parts[i];
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
