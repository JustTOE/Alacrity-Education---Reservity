package dev.tmmc.reservity.reservations.repository;

import dev.tmmc.reservity.reservations.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /**
     * All reservations whose [startsAt, endsAt) interval overlaps the half-open
     * window [dayStart, dayEnd). Mirrors the SpaceClosureRepository pattern.
     */
    @EntityGraph(attributePaths = {"user", "space"})
    @Query("""
        SELECT r FROM Reservation r
         WHERE r.space.id = :spaceId
           AND r.startsAt < :dayEnd
           AND r.endsAt > :dayStart
         ORDER BY r.startsAt
        """)
    List<Reservation> findOverlapping(@Param("spaceId") UUID spaceId,
                                      @Param("dayStart") Instant dayStart,
                                      @Param("dayEnd") Instant dayEnd);

    /** Batch variant for the multi-space availability endpoint. */
    @EntityGraph(attributePaths = {"user", "space"})
    @Query("""
        SELECT r FROM Reservation r
         WHERE r.space.id IN :spaceIds
           AND r.startsAt < :dayEnd
           AND r.endsAt > :dayStart
         ORDER BY r.startsAt
        """)
    List<Reservation> findOverlappingForSpaces(@Param("spaceIds") Collection<UUID> spaceIds,
                                               @Param("dayStart") Instant dayStart,
                                               @Param("dayEnd") Instant dayEnd);

    /** "My reservations" list. Most recent first; pagination handled by caller. */
    @EntityGraph(attributePaths = {"space", "space.building", "request"})
    Page<Reservation> findByUserIdOrderByStartsAtDesc(UUID userId, Pageable pageable);

    /** Upcoming filter for "My reservations" — strictly after `now`. */
    @EntityGraph(attributePaths = {"space", "space.building", "request"})
    Page<Reservation> findByUserIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
            UUID userId, Instant now, Pageable pageable);

    /** Lookup by associated request (to upgrade PENDING → APPROVED). */
    Optional<Reservation> findByRequestId(UUID requestId);

    /** Used by cancel-confirmed-booking to remove the blocking row. */
    void deleteByRequestId(UUID requestId);
}
