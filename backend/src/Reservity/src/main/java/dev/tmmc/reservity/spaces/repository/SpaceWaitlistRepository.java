package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.SpaceWaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpaceWaitlistRepository extends JpaRepository<SpaceWaitlistEntry, UUID> {

    @Query("""
            SELECT w FROM SpaceWaitlistEntry w
            JOIN FETCH w.space s
            JOIN FETCH s.building b
            WHERE w.user.id = :userId
            ORDER BY w.createdAt DESC
            """)
    List<SpaceWaitlistEntry> findAllForUser(UUID userId);

    void deleteByUserIdAndSpaceId(UUID userId, UUID spaceId);
}

