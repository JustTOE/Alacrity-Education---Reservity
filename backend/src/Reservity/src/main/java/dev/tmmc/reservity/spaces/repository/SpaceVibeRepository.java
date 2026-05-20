package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.SpaceVibe;
import dev.tmmc.reservity.spaces.entity.SpaceVibeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceVibeRepository extends JpaRepository<SpaceVibe, SpaceVibeId> {
}
