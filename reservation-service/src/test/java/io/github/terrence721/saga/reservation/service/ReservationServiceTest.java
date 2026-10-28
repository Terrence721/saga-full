package io.github.terrence721.saga.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.reservation.domain.Reservation;
import io.github.terrence721.saga.reservation.domain.ReservationStatus;
import io.github.terrence721.saga.reservation.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.reservation.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.reservation.repository.ReservationRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void setUp() throws Exception {
        service = new ReservationService(repository, kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handleRestaurantApproved_allocatesAndPublishesConfirmation() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.handleRestaurantApproved(new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID()));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

    }

    @Test
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

        verify(repository).save(existing);
        assertThat(existing.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
