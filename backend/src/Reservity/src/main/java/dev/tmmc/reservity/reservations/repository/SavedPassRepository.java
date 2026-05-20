package dev.tmmc.reservity.reservations.repository;

import dev.tmmc.reservity.reservations.entity.SavedPass;
import dev.tmmc.reservity.reservations.entity.SavedPassId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SavedPassRepository extends JpaRepository<SavedPass, SavedPassId> {

    void deleteByReservationId(UUID reservationId);
}
