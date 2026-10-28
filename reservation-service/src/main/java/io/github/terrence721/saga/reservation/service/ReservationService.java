package io.github.terrence721.saga.reservation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.reservation.domain.Reservation;
import io.github.terrence721.saga.reservation.domain.ReservationStatus;
import io.github.terrence721.saga.reservation.dto.CreateReservationRequest;
import io.github.terrence721.saga.reservation.dto.ReservationConfirmedEvent;
import io.github.terrence721.saga.reservation.dto.ReservationRejectedEvent;
import io.github.terrence721.saga.reservation.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.reservation.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.reservation.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReservationService {

    private final ReservationRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.confirmed-topic:reservation-confirmed-topic}")
    private String confirmedTopic;

    @Value("${app.outbox.rejected-topic:reservation-rejected-topic}")
    private String rejectedTopic;

    public ReservationService(ReservationRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Reservation create(CreateReservationRequest request) {
        Reservation reservation = Reservation.builder()
                .customerId(request.customerId())
                .tableNumber(request.tableNumber())
                .partySize(request.partySize())
                .startTime(request.startTime())
                .status(ReservationStatus.CONFIRMED)
                .build();
        return repository.save(java.util.Objects.requireNonNull(reservation));
    }

    public List<Reservation> findAll() {
        return repository.findAll();
    }

    public Reservation findById(@NonNull UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
    }

    public void cancel(@NonNull UUID id) {
        Reservation reservation = findById(id);
        reservation.setStatus(ReservationStatus.CANCELLED);
        repository.save(reservation);
    }

    @Transactional
    public void handleRestaurantApproved(RestaurantApprovedEvent event) {
        try {
            Reservation reservation = repository.findByOrderId(event.orderId()).orElseGet(() -> {
                int tableNumber = Math.abs(event.orderId().hashCode()) % 5 + 1;
                Reservation created = Reservation.builder()
                        .orderId(event.orderId())
                        .customerId(event.customerId().toString())
                        .tableNumber("T-" + tableNumber)
                        .partySize(4)
                        .startTime(java.time.Instant.now())
                        .status(ReservationStatus.CONFIRMED)
                        .build();
                return repository.save(java.util.Objects.requireNonNull(created));
            });
            ReservationConfirmedEvent confirmed = new ReservationConfirmedEvent(
                    event.orderId(), event.customerId(), reservation.getTableNumber(), reservation.getPartySize());
            publish(confirmedTopic, event.orderId().toString(), confirmed);
            log.info("Reservation confirmed for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Reservation allocation failed for order {}: {}", event.orderId(), e.getMessage());
            try {
                publish(rejectedTopic, event.orderId().toString(), new ReservationRejectedEvent(event.orderId(), event.customerId(), "allocation failed"));
            } catch (Exception ignored) {
                // no-op
            }
        }
    }

    @Transactional
    public void handleRestaurantRejected(RestaurantRejectedEvent event) {
        repository.findByOrderId(event.orderId()).ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.CANCELLED);
            repository.save(reservation);
            log.info("Reservation cancelled for rejected order {}", event.orderId());
        });
    }

    private void publish(String topic, String key, Object event) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(
            java.util.Objects.requireNonNull(topic),
            java.util.Objects.requireNonNull(key),
            java.util.Objects.requireNonNull(payload));
    }
}
