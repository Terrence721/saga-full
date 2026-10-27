package io.github.terrence721.saga.reservation.repository;

import io.github.terrence721.saga.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}
