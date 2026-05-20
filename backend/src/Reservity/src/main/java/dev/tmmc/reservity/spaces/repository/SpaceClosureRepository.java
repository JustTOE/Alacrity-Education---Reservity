package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.SpaceClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpaceClosureRepository extends JpaRepository<SpaceClosure, UUID> {

    @Query("""
        SELECT c FROM SpaceClosure c
         WHERE c.space.id = :spaceId
           AND c.startsAt < :dayEnd
           AND c.endsAt > :dayStart
         ORDER BY c.startsAt
        """)
    List<SpaceClosure> findOverlapping(@Param("spaceId") UUID spaceId,
                                       @Param("dayStart") Instant dayStart,
                                       @Param("dayEnd") Instant dayEnd);

    @Query("""
        SELECT c FROM SpaceClosure c
         WHERE c.space.id IN :spaceIds
           AND c.startsAt < :dayEnd
           AND c.endsAt > :dayStart
         ORDER BY c.startsAt
        """)
    List<SpaceClosure> findOverlappingForSpaces(@Param("spaceIds") Collection<UUID> spaceIds,
                                                @Param("dayStart") Instant dayStart,
                                                @Param("dayEnd") Instant dayEnd);
}
