package dev.tmmc.reservity.events.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.dto.EventResponse;
import dev.tmmc.reservity.events.dto.EventSummary;
import dev.tmmc.reservity.events.dto.EventUpdateRequest;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventDisplayStatus;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventStatus;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.event.EventCancelledEvent;
import dev.tmmc.reservity.events.event.EventCreatedEvent;
import dev.tmmc.reservity.events.mapper.EventMapper;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.reservations.entity.Reservation;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.spaces.entity.Building;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.repository.BuildingRepository;
import dev.tmmc.reservity.spaces.repository.SpaceRepository;
import dev.tmmc.reservity.user.entity.AccountType;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final long SOON_WINDOW_MINUTES = 60;
    private static final int MAX_PAGE_SIZE = 100;

    private final EventRepository eventRepository;
    private final EventRsvpRepository rsvpRepository;
    private final EventSlugGenerator slugGenerator;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository orgMembershipRepository;
    private final SpaceRepository spaceRepository;
    private final BuildingRepository buildingRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Event create(EventCreateRequest req, UUID callerUserId) {
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        if (req.startsAt().isBefore(Instant.now().minusSeconds(300))) {
            throw new IllegalArgumentException("startsAt cannot be in the past");
        }

        User caller = userRepository.findById(callerUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", callerUserId));

        UUID hostId = resolveAndValidateHost(req.hostType(), req.hostId(), caller);

        Space space = req.spaceId() == null ? null
                : spaceRepository.findById(req.spaceId())
                        .orElseThrow(() -> new EntityNotFoundException("Space", req.spaceId()));
        Building building = req.buildingId() == null ? null
                : buildingRepository.findById(req.buildingId())
                        .orElseThrow(() -> new EntityNotFoundException("Building", req.buildingId()));

        Reservation reservation = null;
        if (req.reservationId() != null) {
            reservation = reservationRepository.findById(req.reservationId())
                    .orElseThrow(() -> new EntityNotFoundException("Reservation", req.reservationId()));
            if (!reservation.getUser().getId().equals(callerUserId)
                    && caller.getAccountType() != AccountType.ADMIN) {
                throw new ForbiddenOperationException(
                        "Caller does not own this reservation");
            }
            ReservationRequest reqRow = reservation.getRequest();
            if (reqRow == null || reqRow.getStatus() != ReservationStatus.APPROVED) {
                throw new IllegalStatusTransitionException(
                        "Reservation is not APPROVED — cannot host an event on it");
            }
        }

        String slug = slugGenerator.generate(req.title(), req.startsAt());

        Event event = Event.builder()
                .slug(slug)
                .hostType(req.hostType())
                .hostId(hostId)
                .space(space)
                .reservation(reservation)
                .building(building)
                .locationLabel(req.locationLabel())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .title(req.title())
                .blurb(req.blurb())
                .description(req.description())
                .coverImageUrl(req.coverImageUrl())
                .category(req.category() == null ? EventCategory.OTHER : req.category())
                .tag(req.tag())
                .capacity(req.capacity() == null ? 0 : req.capacity())
                .attendeesCount(0)
                .rsvpRequired(Boolean.TRUE.equals(req.rsvpRequired()))
                .visibility(req.visibility() == null ? EventVisibility.PUBLIC : req.visibility())
                .status(EventStatus.PUBLISHED)
                .build();

        Event saved = eventRepository.save(event);
        eventPublisher.publishEvent(new EventCreatedEvent(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummary> list(EventListFilters filters, UUID callerUserIdOrNull,
                                           int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.ASC, "startsAt"));
        Specification<Event> spec = buildSpec(filters, callerUserIdOrNull);
        Page<Event> result = eventRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(eventMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public EventResponse getBySlugOrId(String slugOrId, UUID callerUserIdOrNull) {
        Event event = resolveBySlugOrId(slugOrId);
        if (!isVisibleTo(event, callerUserIdOrNull)) {
            throw new EntityNotFoundException("Event", slugOrId);
        }
        RsvpStatus callerRsvp = callerUserIdOrNull == null
                ? null
                : rsvpRepository.findByEventIdAndUserId(event.getId(), callerUserIdOrNull)
                        .map(r -> r.getStatus())
                        .orElse(null);
        return eventMapper.toResponse(event, callerRsvp);
    }

    @Transactional
    public Event update(UUID eventId, EventUpdateRequest req, UUID callerUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
        requireHostWriteAccess(event, callerUserId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStatusTransitionException(
                    "Cannot edit a cancelled event");
        }

        if (req.title() != null) event.setTitle(req.title());
        if (req.blurb() != null) event.setBlurb(req.blurb());
        if (req.description() != null) event.setDescription(req.description());
        if (req.coverImageUrl() != null) event.setCoverImageUrl(req.coverImageUrl());
        if (req.startsAt() != null) event.setStartsAt(req.startsAt());
        if (req.endsAt() != null) event.setEndsAt(req.endsAt());
        if (req.startsAt() != null || req.endsAt() != null) {
            if (!event.getEndsAt().isAfter(event.getStartsAt())) {
                throw new IllegalArgumentException("endsAt must be after startsAt");
            }
        }
        if (req.spaceId() != null) {
            event.setSpace(spaceRepository.findById(req.spaceId())
                    .orElseThrow(() -> new EntityNotFoundException("Space", req.spaceId())));
        }
        if (req.buildingId() != null) {
            event.setBuilding(buildingRepository.findById(req.buildingId())
                    .orElseThrow(() -> new EntityNotFoundException("Building", req.buildingId())));
        }
        if (req.locationLabel() != null) event.setLocationLabel(req.locationLabel());
        if (req.category() != null) event.setCategory(req.category());
        if (req.tag() != null) event.setTag(req.tag());
        if (req.capacity() != null) event.setCapacity(req.capacity());
        if (req.rsvpRequired() != null) event.setRsvpRequired(req.rsvpRequired());
        if (req.visibility() != null) event.setVisibility(req.visibility());

        return eventRepository.save(event);
    }

    @Transactional
    public Event cancel(UUID eventId, String reason, UUID callerUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
        requireHostWriteAccess(event, callerUserId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStatusTransitionException("Event already cancelled");
        }

        List<UUID> notifyUserIds = rsvpRepository.findNonCancelledUserIds(eventId);

        event.setStatus(EventStatus.CANCELLED);
        event.setCancelledAt(Instant.now());
        event.setCancellationReason(reason);
        Event saved = eventRepository.save(event);

        eventPublisher.publishEvent(new EventCancelledEvent(saved.getId(), notifyUserIds));
        return saved;
    }

    /** Public so {@link EventRsvpService} can validate caller-host parity. */
    public boolean isHost(Event event, UUID callerUserId) {
        if (callerUserId == null) return false;
        if (event.getHostType() == EventHostType.USER) {
            return event.getHostId().equals(callerUserId);
        }
        return userRepository.findById(callerUserId)
                .map(u -> orgIsHostedByCaller(event.getHostId(), u))
                .orElse(false);
    }

    public Event requireById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
    }

    private boolean orgIsHostedByCaller(UUID orgId, User caller) {
        Optional<Organization> org = organizationRepository.findById(orgId);
        return org.isPresent()
                && orgMembershipRepository.existsByUserAndOrganization(caller, org.get());
    }

    void requireHostWriteAccess(Event event, UUID callerUserId) {
        User caller = userRepository.findById(callerUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", callerUserId));
        if (caller.getAccountType() == AccountType.ADMIN) return;
        if (event.getHostType() == EventHostType.USER) {
            if (event.getHostId().equals(callerUserId)) return;
        } else if (event.getHostType() == EventHostType.ORGANIZATION) {
            if (orgIsHostedByCaller(event.getHostId(), caller)) return;
        }
        throw new ForbiddenOperationException(
                "Only the event host can modify this event");
    }

    private UUID resolveAndValidateHost(EventHostType type, UUID requestedHostId, User caller) {
        if (caller.getAccountType() == AccountType.ADMIN && requestedHostId != null) {
            // Admin can host on behalf of any USER or ORGANIZATION.
            if (type == EventHostType.USER) {
                userRepository.findById(requestedHostId)
                        .orElseThrow(() -> new EntityNotFoundException("User", requestedHostId));
                return requestedHostId;
            }
            organizationRepository.findById(requestedHostId)
                    .orElseThrow(() -> new EntityNotFoundException("Organization", requestedHostId));
            return requestedHostId;
        }
        if (type == EventHostType.USER) {
            UUID resolved = requestedHostId == null ? caller.getId() : requestedHostId;
            if (!resolved.equals(caller.getId())) {
                throw new ForbiddenOperationException(
                        "Cannot host an event as a different user");
            }
            return resolved;
        }
        if (requestedHostId == null) {
            throw new IllegalArgumentException("hostId is required for ORGANIZATION host");
        }
        Organization org = organizationRepository.findById(requestedHostId)
                .orElseThrow(() -> new EntityNotFoundException("Organization", requestedHostId));
        if (!orgMembershipRepository.existsByUserAndOrganization(caller, org)) {
            throw new ForbiddenOperationException(
                    "Caller is not a member of the host organization");
        }
        return requestedHostId;
    }

    private Event resolveBySlugOrId(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return eventRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Event", slugOrId));
        } catch (IllegalArgumentException ignored) {
            return eventRepository.findBySlug(slugOrId)
                    .orElseThrow(() -> new EntityNotFoundException("Event", slugOrId));
        }
    }

    /**
     * Visibility rules (mirrors §M8-NOW C):
     * <ul>
     *   <li>{@code PUBLIC}: visible to everyone.</li>
     *   <li>{@code UNLISTED}: visible by direct slug/id link to anyone.</li>
     *   <li>{@code PRIVATE}: visible only to host or RSVP'd users.</li>
     * </ul>
     */
    private boolean isVisibleTo(Event event, UUID callerUserIdOrNull) {
        if (event.getVisibility() == EventVisibility.PUBLIC
                || event.getVisibility() == EventVisibility.UNLISTED) {
            return true;
        }
        if (callerUserIdOrNull == null) return false;
        if (isHost(event, callerUserIdOrNull)) return true;
        return rsvpRepository.existsByEventIdAndUserId(event.getId(), callerUserIdOrNull);
    }

    private Specification<Event> buildSpec(EventListFilters f, UUID callerUserIdOrNull) {
        Instant now = Instant.now();
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            // Visibility filter:
            //   anonymous → only PUBLIC.
            //   authenticated → PUBLIC OR (host == caller) OR (caller has RSVP).
            // PRIVATE never shows in list — only direct GET.
            if (callerUserIdOrNull == null) {
                preds.add(cb.equal(root.get("visibility"), EventVisibility.PUBLIC));
            } else {
                Predicate isPublic = cb.equal(root.get("visibility"), EventVisibility.PUBLIC);
                Predicate isUnlistedHostOrRsvp = cb.and(
                        cb.equal(root.get("visibility"), EventVisibility.UNLISTED),
                        cb.or(
                                hostMatches(root, cb, callerUserIdOrNull),
                                hasRsvp(root, query, cb, callerUserIdOrNull)));
                preds.add(cb.or(isPublic, isUnlistedHostOrRsvp));
            }
            // Lifecycle (default: exclude DRAFT)
            preds.add(cb.notEqual(root.get("status"), EventStatus.DRAFT));

            if (f.category() != null) {
                preds.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.hostType() != null) {
                preds.add(cb.equal(root.get("hostType"), f.hostType()));
            }
            if (f.hostId() != null) {
                preds.add(cb.equal(root.get("hostId"), f.hostId()));
            }
            if (f.spaceId() != null) {
                preds.add(cb.equal(root.get("space").get("id"), f.spaceId()));
            }
            if (f.buildingId() != null) {
                preds.add(cb.equal(root.get("building").get("id"), f.buildingId()));
            }
            if (f.displayStatus() != null) {
                applyDisplayStatusPredicate(preds, root, cb, f.displayStatus(), now);
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private static Predicate hostMatches(jakarta.persistence.criteria.Root<Event> root,
                                         jakarta.persistence.criteria.CriteriaBuilder cb,
                                         UUID userId) {
        return cb.and(
                cb.equal(root.get("hostType"), EventHostType.USER),
                cb.equal(root.get("hostId"), userId));
    }

    private static Predicate hasRsvp(jakarta.persistence.criteria.Root<Event> root,
                                     jakarta.persistence.criteria.AbstractQuery<?> query,
                                     jakarta.persistence.criteria.CriteriaBuilder cb,
                                     UUID userId) {
        var sub = query.subquery(UUID.class);
        var rsvpRoot = sub.from(dev.tmmc.reservity.events.entity.EventRsvp.class);
        sub.select(rsvpRoot.get("event").get("id"))
                .where(cb.and(
                        cb.equal(rsvpRoot.get("user").get("id"), userId),
                        cb.equal(rsvpRoot.get("event").get("id"), root.get("id")),
                        cb.notEqual(rsvpRoot.get("status"), RsvpStatus.CANCELLED)));
        return cb.exists(sub);
    }

    private static void applyDisplayStatusPredicate(
            List<Predicate> preds,
            jakarta.persistence.criteria.Root<Event> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            EventDisplayStatus s,
            Instant now) {
        Instant soonEdge = now.plus(Duration.ofMinutes(SOON_WINDOW_MINUTES));
        LocalDate todayUtc = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant tomorrowStart = todayUtc.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant tomorrowEnd = todayUtc.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();
        switch (s) {
            case LIVE -> {
                preds.add(cb.lessThanOrEqualTo(root.get("startsAt"), now));
                preds.add(cb.greaterThan(root.get("endsAt"), now));
            }
            case SOON -> {
                preds.add(cb.greaterThan(root.get("startsAt"), now));
                preds.add(cb.lessThanOrEqualTo(root.get("startsAt"), soonEdge));
            }
            case TOMORROW -> {
                preds.add(cb.greaterThan(root.get("startsAt"), soonEdge));
                preds.add(cb.greaterThanOrEqualTo(root.get("startsAt"), tomorrowStart));
                preds.add(cb.lessThan(root.get("startsAt"), tomorrowEnd));
            }
            case UPCOMING -> {
                preds.add(cb.greaterThan(root.get("startsAt"), soonEdge));
                preds.add(cb.greaterThanOrEqualTo(root.get("startsAt"), tomorrowEnd));
            }
            case PAST -> preds.add(cb.lessThanOrEqualTo(root.get("endsAt"), now));
        }
    }

    /** Filters for the list endpoint. All fields nullable. */
    public record EventListFilters(
            EventDisplayStatus displayStatus,
            EventCategory category,
            EventHostType hostType,
            UUID hostId,
            UUID spaceId,
            UUID buildingId
    ) {
        public static EventListFilters empty() {
            return new EventListFilters(null, null, null, null, null, null);
        }
    }
}
