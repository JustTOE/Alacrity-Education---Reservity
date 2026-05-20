package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.SpaceFavorite;
import dev.tmmc.reservity.spaces.entity.SpaceFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpaceFavoriteRepository extends JpaRepository<SpaceFavorite, SpaceFavoriteId> {

    @Query("""
            SELECT sf FROM SpaceFavorite sf
            JOIN FETCH sf.space s
            JOIN FETCH s.building b
            WHERE sf.user.id = :userId
            ORDER BY sf.createdAt DESC
            """)
    List<SpaceFavorite> findAllForUser(UUID userId);

    boolean existsByIdUserIdAndIdSpaceId(UUID userId, UUID spaceId);

    void deleteByIdUserIdAndIdSpaceId(UUID userId, UUID spaceId);
}
