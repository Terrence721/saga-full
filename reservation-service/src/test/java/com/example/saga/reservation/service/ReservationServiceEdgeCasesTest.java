package com.example.saga.reservation.service;

import com.example.saga.reservation.domain.ReservationStatus;
import com.example.saga.reservation.dto.CreateReservationRequest;
import com.example.saga.reservation.dto.RestaurantApprovedEvent;
import com.example.saga.reservation.dto.RestaurantRejectedEvent;
import com.example.saga.reservation.repository.OutboxRepository;
import com.example.saga.reservation.repository.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceEdgeCasesTest {

    @Mock
    private ReservationRepository repository;

    @Mock
    private OutboxRepository outboxRepository;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(repository, outboxRepository, new OutboxRecordFactory(new ObjectMapper()));
    }

    @Test
    void create_throwsWhenRequestIsNull() {
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsWhenCustomerIdIsNull() {
        CreateReservationRequest request = new CreateReservationRequest(null, "T-1", 4,
                Instant.parse("2026-10-28T18:00:00Z"));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleRestaurantApproved_throwsWhenEventIsNull() {
        assertThatThrownBy(() -> service.handleRestaurantApproved(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleRestaurantApproved_throwsWhenOrderIdIsNull() {
        UUID customerId = UUID.randomUUID();
        assertThatThrownBy(() -> service.handleRestaurantApproved(new RestaurantApprovedEvent(null, customerId, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleRestaurantApproved_throwsWhenCustomerIdIsNull() {
        UUID orderId = UUID.randomUUID();
        assertThatThrownBy(() -> service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, null, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("null")
    void handleRestaurantApproved_swallowsOutboxFailureDuringRejection() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(new IllegalStateException("db down"));
        when(outboxRepository.save(any())).thenThrow(new RuntimeException("outbox down"));

        org.assertj.core.api.Assertions.assertThatCode(() ->
                service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID())))
                .doesNotThrowAnyException();
    }

    @Test
    void handleRestaurantRejected_throwsWhenEventIsNull() {
        assertThatThrownBy(() -> service.handleRestaurantRejected(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleRestaurantRejected_throwsWhenOrderIdIsNull() {
        UUID customerId = UUID.randomUUID();
        assertThatThrownBy(() -> service.handleRestaurantRejected(new RestaurantRejectedEvent(null, customerId, "reason")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
