package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    Optional<Building> findByName(String name);

    Optional<Building> findByShortCodeIgnoreCase(String shortCode);
}
