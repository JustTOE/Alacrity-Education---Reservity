package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceImageRepository extends JpaRepository<SpaceImage, UUID> {

    List<SpaceImage> findBySpaceIdOrderByDisplayOrderAsc(UUID spaceId);

    long countBySpaceId(UUID spaceId);

    Optional<SpaceImage> findFirstBySpaceIdOrderByDisplayOrderAsc(UUID spaceId);

    Optional<SpaceImage> findFirstBySpaceIdAndIdNotOrderByDisplayOrderAsc(UUID spaceId, UUID excludedId);

    List<SpaceImage> findBySpaceIdInAndPrimaryTrue(Collection<UUID> spaceIds);
}
