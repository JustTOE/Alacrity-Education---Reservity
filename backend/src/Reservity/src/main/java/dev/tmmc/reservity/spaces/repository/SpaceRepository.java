package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository extends JpaRepository<Space, UUID>, JpaSpecificationExecutor<Space> {

    @EntityGraph(attributePaths = {"building", "spaceVibes", "spaceVibes.vibe", "images"})
    Optional<Space> findBySlug(String slug);

    @EntityGraph(attributePaths = {"building", "spaceVibes", "spaceVibes.vibe", "images"})
    Optional<Space> findByIdAndDeletedAtIsNull(UUID id);

    /** Spaces this user owns directly (owner_type=USER). */
    @Query("""
        SELECT s.id FROM Space s
         WHERE s.ownerType = :type
           AND s.ownerId = :userId
           AND s.deletedAt IS NULL
        """)
    List<UUID> findIdsOwnedByUser(@Param("userId") UUID userId, @Param("type") SpaceOwnerType type);

    /** Spaces owned by any of the given organizations (owner_type=ORGANIZATION). */
    @Query("""
        SELECT s.id FROM Space s
         WHERE s.ownerType = :type
           AND s.ownerId IN :orgIds
           AND s.deletedAt IS NULL
        """)
    List<UUID> findIdsOwnedByOrgs(@Param("orgIds") Collection<UUID> orgIds, @Param("type") SpaceOwnerType type);
}
