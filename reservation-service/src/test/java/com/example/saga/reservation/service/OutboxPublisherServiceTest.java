package com.example.saga.reservation.service;

import com.example.saga.reservation.domain.OutboxRecord;
import com.example.saga.reservation.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "null"})
class OutboxPublisherServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherService publisherService;

    @BeforeEach
    void setUp() {
        publisherService = new OutboxPublisherService(
                outboxRepository, kafkaTemplate, "reservation-confirmed-topic", "reservation-rejected-topic", 10, 10_000L);
    }

    private OutboxRecord outboxRecord(String aggregateId, String eventType) {
        return OutboxRecord.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload("{\"orderId\":\"" + aggregateId + "\"}")
                .createdTime(LocalDateTime.now())
                .build();
    }

    @Test
    void publishPendingOutboxRecords_doesNothing_whenNoRecords() {
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(Collections.emptyList());

        publisherService.publishPendingOutboxRecords();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).delete(any());
    }

    @Test
    void publishPendingOutboxRecords_routesToConfirmedTopic_forReservationConfirmedEvent() {
        OutboxRecord record = outboxRecord("order-1", "ReservationConfirmedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisherService.publishPendingOutboxRecords();

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("reservation-confirmed-topic");
        verify(outboxRepository).delete(record);
    }

    @Test
    void publishPendingOutboxRecords_routesToRejectedTopic_forReservationRejectedEvent() {
        OutboxRecord record = outboxRecord("order-2", "ReservationRejectedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisherService.publishPendingOutboxRecords();

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("reservation-rejected-topic");
        verify(outboxRepository).delete(record);
    }

    @Test
    void publishPendingOutboxRecords_skipsUnknownEventType() {
        OutboxRecord record = outboxRecord("order-3", "SomeUnknownEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));

        publisherService.publishPendingOutboxRecords();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).delete(any());
    }
}
