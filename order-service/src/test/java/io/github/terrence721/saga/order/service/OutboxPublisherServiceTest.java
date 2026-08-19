package io.github.terrence721.saga.order.service;

import io.github.terrence721.saga.order.domain.OutboxRecord;
import io.github.terrence721.saga.order.repository.OutboxRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OutboxPublisherServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherService publisherService;

    @BeforeEach
    void setUp() {
        publisherService = new OutboxPublisherService(outboxRepository, kafkaTemplate, "order-created-topic", 10, 10_000L);
    }

    private OutboxRecord outboxRecord(String aggregateId, String payload) {
        return OutboxRecord.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType("OrderCreatedEvent")
                .payload(payload)
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
    void publishPendingOutboxRecords_publishesRawPayloadAndDeletes_onSuccess() {
        OutboxRecord record = outboxRecord("order-123", "{\"orderId\":\"order-123\"}");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisherService.publishPendingOutboxRecords();

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message sent = messageCaptor.getValue();
        assertThat(sent.getPayload()).isEqualTo(record.getPayload());
        assertThat(sent.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("order-created-topic");
        assertThat(sent.getHeaders().get(KafkaHeaders.KEY)).isEqualTo("order-123");
        assertThat(sent.getHeaders().get("eventType")).isEqualTo("OrderCreatedEvent");

        verify(outboxRepository).delete(record);
    }

    @Test
    void publishPendingOutboxRecords_leavesRecordForRetry_onFailure() {
        OutboxRecord record = outboxRecord("order-fail", "{}");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));

        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(failed);

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(any());
    }

    @Test
    void publishPendingOutboxRecords_leavesRecordForRetry_onTimeout() {
        OutboxPublisherService shortTimeoutService =
                new OutboxPublisherService(outboxRepository, kafkaTemplate, "order-created-topic", 10, 50L);
        OutboxRecord record = outboxRecord("order-slow", "{}");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        // Never completes - proves the explicit get(timeout) bound fires rather than blocking
        // indefinitely on Kafka's own delivery.timeout.ms default.
        when(kafkaTemplate.send(any(Message.class))).thenReturn(new CompletableFuture<>());

        shortTimeoutService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(any());
    }

    @Test
    void publishPendingOutboxRecords_continuesBatch_afterOneRecordFails() {
        OutboxRecord failing = outboxRecord("order-fail", "{}");
        OutboxRecord succeeding = outboxRecord("order-ok", "{}");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(failing, succeeding));

        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(argThat((Message<?> message) ->
                message != null && "order-fail".equals(message.getHeaders().get(KafkaHeaders.KEY)))))
                .thenReturn(failed);
        when(kafkaTemplate.send(argThat((Message<?> message) ->
                message != null && "order-ok".equals(message.getHeaders().get(KafkaHeaders.KEY)))))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(failing);
        verify(outboxRepository).delete(succeeding);
    }
}
