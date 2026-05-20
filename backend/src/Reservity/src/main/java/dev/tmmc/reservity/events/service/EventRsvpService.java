package dev.tmmc.reservity.events.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.events.dto.EventAttendeeSummary;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.EventRsvp;
import dev.tmmc.reservity.events.entity.enums.EventStatus;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.event.RsvpPromotedEvent;
import dev.tmmc.reservity.events.mapper.EventRsvpMapper;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * RSVP business logic. The hot path is {@link #rsvp(UUID, UUID, RsvpStatus)},
 * which:
 * <ol>
 *   <li>Acquires a pessimistic write lock on the event row to serialize
 *       capacity decisions across concurrent requests.</li>
 *   <li>Determines effective status: {@code GOING} downgrades to
 *       {@code WAITLIST} when {@code attendees_count >= capacity}.</li>
 *   <li>Updates the denormalized {@code attendees_count} on the event so
 *       the landing-page board reads remain a single column lookup.</li>
 *   <li>If a {@code GOING} user just left, auto-promotes the oldest
 *       {@code WAITLIST} row to {@code GOING} and publishes
 *       {@link RsvpPromotedEvent}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class EventRsvpService {

    private static final int MAX_PAGE_SIZE = 100;

    /** Statuses a client may request directly. WAITLIST is server-assigned; CANCELLED comes via DELETE. */
    private static final Set<RsvpStatus> CLIENT_REQUESTABLE = Set.of(
            RsvpStatus.GOING, RsvpStatus.INTERESTED, RsvpStatus.CANT_GO);

    private final EventRepository eventRepository;
    private final EventRsvpRepository rsvpRepository;
    private final UserRepository userRepository;
    private final EventRsvpMapper rsvpMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public EventRsvp rsvp(UUID eventId, UUID userId, RsvpStatus requested) {
        if (requested == null || !CLIENT_REQUESTABLE.contains(requested)) {
            throw new IllegalArgumentException(
                    "RSVP status must be one of GOING, INTERESTED, CANT_GO");
        }
        return applyRsvp(eventId, userId, requested);
    }

    /** DELETE /api/events/{id}/rsvp shorthand for "set my status to CANCELLED". */
    @Transactional
    public void cancel(UUID eventId, UUID userId) {
        applyRsvp(eventId, userId, RsvpStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventAttendeeSummary> listAttendees(UUID eventId, RsvpStatus status,
                                                            int page, int size) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        RsvpStatus filter = status == null ? RsvpStatus.GOING : status;
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize);
        Page<EventRsvp> result = rsvpRepository.findByEventIdAndStatus(eventId, filter, pageable);
        return PageResponse.of(result.map(rsvpMapper::toAttendeeSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<EventRsvp> listForUser(UUID userId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize);
        return PageResponse.of(rsvpRepository.findActiveForUser(userId, pageable));
    }

    private EventRsvp applyRsvp(UUID eventId, UUID userId, RsvpStatus desiredOrCancel) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStatusTransitionException(
                    "Event is cancelled — no RSVPs accepted");
        }
        if (!event.getEndsAt().isAfter(Instant.now())) {
            throw new IllegalStatusTransitionException(
                    "Event has ended — no RSVPs accepted");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        Optional<EventRsvp> existingOpt = rsvpRepository.findByEventIdAndUserId(eventId, userId);
        EventRsvp rsvp;
        RsvpStatus prior;
        if (existingOpt.isPresent()) {
            rsvp = existingOpt.get();
            prior = rsvp.getStatus();
        } else {
            rsvp = EventRsvp.builder()
                    .event(event)
                    .user(user)
                    .status(RsvpStatus.GOING) // overwritten below
                    .build();
            prior = null;
        }

        RsvpStatus effective = resolveEffectiveStatus(event, prior, desiredOrCancel);

        // Adjust attendees_count for the transition.
        boolean wasGoing = prior == RsvpStatus.GOING;
        boolean nowGoing = effective == RsvpStatus.GOING;
        if (wasGoing && !nowGoing) {
            event.setAttendeesCount(event.getAttendeesCount() - 1);
        } else if (!wasGoing && nowGoing) {
            event.setAttendeesCount(event.getAttendeesCount() + 1);
        }
        rsvp.setStatus(effective);
        EventRsvp saved = rsvpRepository.save(rsvp);
        eventRepository.save(event);

        // Auto-promote on freed slot.
        if (wasGoing && !nowGoing && event.getCapacity() > 0
                && event.getAttendeesCount() < event.getCapacity()) {
            promoteOldestWaitlist(event);
        }
        return saved;
    }

    private RsvpStatus resolveEffectiveStatus(Event event, RsvpStatus prior, RsvpStatus desired) {
        if (desired != RsvpStatus.GOING) return desired; // CANT_GO, CANCELLED, INTERESTED pass through
        // Already GOING: idempotent — no double-count concern.
        if (prior == RsvpStatus.GOING) return RsvpStatus.GOING;
        if (event.getCapacity() == 0) return RsvpStatus.GOING; // unlimited
        if (event.getAttendeesCount() < event.getCapacity()) return RsvpStatus.GOING;
        return RsvpStatus.WAITLIST;
    }

    private void promoteOldestWaitlist(Event event) {
        Optional<EventRsvp> next = rsvpRepository
                .findFirstByEventIdAndStatusOrderByCreatedAtAsc(event.getId(), RsvpStatus.WAITLIST);
        if (next.isEmpty()) return;
        EventRsvp picked = next.get();
        picked.setStatus(RsvpStatus.GOING);
        rsvpRepository.save(picked);
        event.setAttendeesCount(event.getAttendeesCount() + 1);
        eventRepository.save(event);
        eventPublisher.publishEvent(new RsvpPromotedEvent(
                event.getId(), picked.getUser().getId(), event.getTitle()));
    }
}
