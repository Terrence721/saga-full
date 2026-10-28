package com.example.saga.reservation.repository;

import com.example.saga.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByOrderId(UUID orderId);
}
