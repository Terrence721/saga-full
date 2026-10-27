package io.github.terrence721.saga.reservation.service;

import io.github.terrence721.saga.reservation.domain.Reservation;
import io.github.terrence721.saga.reservation.domain.ReservationStatus;
import io.github.terrence721.saga.reservation.dto.CreateReservationRequest;
import io.github.terrence721.saga.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public Reservation create(CreateReservationRequest request) {
        Reservation reservation = Reservation.builder()
                .customerId(request.customerId())
                .tableNumber(request.tableNumber())
                .partySize(request.partySize())
                .startTime(request.startTime())
                .status(ReservationStatus.CONFIRMED)
                .build();
        return repository.save(reservation);
    }

    public List<Reservation> findAll() {
        return repository.findAll();
    }

    public Reservation findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
    }

    public void cancel(UUID id) {
        Reservation reservation = findById(id);
        reservation.setStatus(ReservationStatus.CANCELLED);
        repository.save(reservation);
    }
}
