package dev.tmmc.reservity.reservations.repository;

import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, UUID> {

    Optional<ReservationRequest> findByReservationCode(String code);

    boolean existsByReservationCode(String code);

    @EntityGraph(attributePaths = {"space", "space.building"})
    Page<ReservationRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    @EntityGraph(attributePaths = {"space", "space.building"})
    Page<ReservationRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId, ReservationStatus status, Pageable pageable);

    /** Owner queue: requests on spaces this user owns. Filtered IN-list version. */
    @EntityGraph(attributePaths = {"space", "space.building", "requester"})
    @Query("""
        SELECT r FROM ReservationRequest r
         WHERE r.space.id IN :spaceIds
         ORDER BY r.createdAt DESC
        """)
    Page<ReservationRequest> findReceivedForSpaces(@Param("spaceIds") List<UUID> spaceIds, Pageable pageable);

    @EntityGraph(attributePaths = {"space", "space.building", "requester"})
    @Query("""
        SELECT r FROM ReservationRequest r
         WHERE r.space.id IN :spaceIds
           AND r.status = :status
         ORDER BY r.createdAt DESC
        """)
    Page<ReservationRequest> findReceivedForSpacesByStatus(@Param("spaceIds") List<UUID> spaceIds,
                                                           @Param("status") ReservationStatus status,
                                                           Pageable pageable);
}
