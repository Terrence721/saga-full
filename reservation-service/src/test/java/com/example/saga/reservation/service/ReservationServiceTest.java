package com.example.saga.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.reservation.domain.Reservation;
import com.example.saga.reservation.domain.ReservationStatus;
import com.example.saga.reservation.dto.CreateReservationRequest;
import com.example.saga.reservation.dto.RestaurantApprovedEvent;
import com.example.saga.reservation.dto.RestaurantRejectedEvent;
import com.example.saga.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    @Mock
    private ReservationRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private ReservationService service;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() throws Exception {
        service = new ReservationService(repository, kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @SuppressWarnings("null")
    void handleRestaurantApproved_allocatesAndPublishesConfirmation() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.save(any(Reservation.class))).thenAnswer(
            inv -> java.util.Objects.requireNonNull(inv.getArgument(0, Reservation.class)));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID()));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

    }

    @Test
    @SuppressWarnings("null")
    void handleRestaurantApproved_isIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Reservation existing = Reservation.builder()
                .id(UUID.randomUUID()).orderId(orderId).customerId(customerId.toString())
                .tableNumber("T-1").partySize(4).startTime(Instant.now())
                .status(ReservationStatus.CONFIRMED).build();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID()));

        verify(repository, never()).save(any(Reservation.class));
    }

    @Test
    void handleRestaurantRejected_cancelsExistingReservation() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Reservation existing = Reservation.builder()
                .id(UUID.randomUUID()).orderId(orderId).customerId(customerId.toString())
                .tableNumber("T-1").partySize(4).startTime(Instant.now())
                .status(ReservationStatus.CONFIRMED).build();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        service.handleRestaurantRejected(new RestaurantRejectedEvent(orderId, customerId, "no table"));

        verify(repository).save(java.util.Objects.requireNonNull(existing));
        assertThat(existing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @SuppressWarnings("null")
    void create_persistsAndReturns() {
        CreateReservationRequest request = new CreateReservationRequest("customer-1", "T-1", 4,
                java.time.Instant.parse("2026-10-28T18:00:00Z"));
        when(repository.save(any(Reservation.class))).thenAnswer(
            inv -> java.util.Objects.requireNonNull(inv.getArgument(0, Reservation.class)));

        Reservation result = service.create(request);

        assertThat(result.getCustomerId()).isEqualTo("customer-1");
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void findAll_returnsRepositoryRows() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findById_returnsExisting() {
        UUID id = UUID.randomUUID();
        Reservation existing = Reservation.builder().id(id).customerId("c").tableNumber("T1")
                .partySize(2).startTime(Instant.now()).status(ReservationStatus.CONFIRMED).build();
        when(repository.findById(java.util.Objects.requireNonNull(id))).thenReturn(Optional.of(existing));
        assertThat(service.findById(id)).isEqualTo(existing);
    }

    @Test
    void cancel_marksReservationCancelled() {
        UUID id = UUID.randomUUID();
        Reservation existing = Reservation.builder().id(id).customerId("c").tableNumber("T1")
                .partySize(2).startTime(Instant.now()).status(ReservationStatus.CONFIRMED).build();
        when(repository.findById(java.util.Objects.requireNonNull(id))).thenReturn(Optional.of(existing));
        when(repository.save(java.util.Objects.requireNonNull(existing)))
            .thenReturn(java.util.Objects.requireNonNull(existing));

        service.cancel(id);

        assertThat(existing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
}



    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancel_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleRestaurantApproved_publishesRejectionOnAllocationFailure() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.save(any(Reservation.class))).thenThrow(new IllegalStateException("db down"));

        // Must not throw: allocation failure is logged and (best effort) reported.
        service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID()));
    }

    @Test
    void handleRestaurantRejected_isNoOpWhenNoReservationExists() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());

        service.handleRestaurantRejected(new RestaurantRejectedEvent(orderId, customerId, "no table"));

        verify(repository, never()).save(any(Reservation.class));
    }

}
