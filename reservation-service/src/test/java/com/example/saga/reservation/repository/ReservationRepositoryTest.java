package com.example.saga.reservation.repository;

import com.example.saga.reservation.domain.Reservation;
import com.example.saga.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository repository;

    @Test
    void saveAndFindByOrderId_roundTrips() {
        UUID orderId = UUID.randomUUID();
        Reservation saved = repository.save(Reservation.builder()
                .orderId(orderId)
                .customerId("customer-1")
                .tableNumber("T-1")
                .partySize(4)
                .startTime(Instant.parse("2026-10-28T18:00:00Z"))
                .status(ReservationStatus.CONFIRMED)
                .build());

        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findByOrderId(orderId)).isPresent()
                .get().extracting(Reservation::getTableNumber).isEqualTo("T-1");
    }

    @Test
    void findAll_returnsPersistedRows() {
        repository.save(Reservation.builder()
                .customerId("customer-2")
                .tableNumber("T-2")
                .partySize(2)
                .startTime(Instant.now())
                .status(ReservationStatus.CANCELLED)
                .build());

        assertThat(repository.count()).isPositive();
    }
}
