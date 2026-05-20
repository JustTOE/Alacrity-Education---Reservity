package dev.tmmc.reservity.reservations.repository;

import dev.tmmc.reservity.reservations.entity.ReservationSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationSeriesRepository extends JpaRepository<ReservationSeries, UUID> {
}
