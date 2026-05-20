package dev.tmmc.reservity.events.repository;

import dev.tmmc.reservity.events.entity.EventRsvp;
import dev.tmmc.reservity.events.entity.EventRsvpId;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRsvpRepository
        extends JpaRepository<EventRsvp, EventRsvpId> {

    Optional<EventRsvp> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    /** Auto-promote query: oldest WAITLIST row for this event. */
    Optional<EventRsvp> findFirstByEventIdAndStatusOrderByCreatedAtAsc(
            UUID eventId, RsvpStatus status);

    /** All non-CANCELLED RSVPs for "notify everyone the event was cancelled". */
    @Query("""
            SELECT r.user.id
              FROM EventRsvp r
             WHERE r.event.id = :eventId
               AND r.status <> dev.tmmc.reservity.events.entity.enums.RsvpStatus.CANCELLED
            """)
    List<UUID> findNonCancelledUserIds(@Param("eventId") UUID eventId);

    /** Paginated attendees for /api/events/{id}/attendees, filterable by status. */
    Page<EventRsvp> findByEventIdAndStatus(UUID eventId, RsvpStatus status, Pageable pageable);

    /** "My events" hot path — every status except CANCELLED. */
    @Query("""
            SELECT r
              FROM EventRsvp r
             WHERE r.user.id = :userId
               AND r.status <> dev.tmmc.reservity.events.entity.enums.RsvpStatus.CANCELLED
             ORDER BY r.createdAt DESC
            """)
    Page<EventRsvp> findActiveForUser(@Param("userId") UUID userId, Pageable pageable);
}
