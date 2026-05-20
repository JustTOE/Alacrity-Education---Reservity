package dev.tmmc.reservity.spaces.repository;

import dev.tmmc.reservity.spaces.entity.Vibe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VibeRepository extends JpaRepository<Vibe, String> {

    List<Vibe> findAllByActiveTrueOrderByDisplayOrderAsc();
}
