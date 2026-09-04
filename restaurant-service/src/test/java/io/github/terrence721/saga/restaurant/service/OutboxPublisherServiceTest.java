package io.github.terrence721.saga.restaurant.service;

import io.github.terrence721.saga.restaurant.domain.OutboxRecord;
import io.github.terrence721.saga.restaurant.repository.OutboxRepository;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "null"}) // test fixtures/mocks here are always real, non-null values.
class OutboxPublisherServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherService publisherService;

    @BeforeEach
    void setUp() {
        publisherService = new OutboxPublisherService(
                outboxRepository, kafkaTemplate, "restaurant-approved-topic", "restaurant-rejected-topic", 10, 10_000L);
    }

    private OutboxRecord outboxRecord(String aggregateId, String eventType) {
        OutboxRecord record = OutboxRecord.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload("{\"orderId\":\"" + aggregateId + "\"}")
                .createdTime(LocalDateTime.now())
                .build();
        return record;
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
    void publishPendingOutboxRecords_routesToApprovedTopic_forRestaurantApprovedEvent() {
        OutboxRecord record = outboxRecord("order-1", "RestaurantApprovedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisherService.publishPendingOutboxRecords();

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("restaurant-approved-topic");
        verify(outboxRepository).delete(record);
    }

    @Test
    void publishPendingOutboxRecords_routesToRejectedTopic_forRestaurantRejectedEvent() {
        OutboxRecord record = outboxRecord("order-2", "RestaurantRejectedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisherService.publishPendingOutboxRecords();

        verify(kafkaTemplate).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("restaurant-rejected-topic");
        verify(outboxRepository).delete(record);
    }

    @Test
    void publishPendingOutboxRecords_throwsIllegalArgumentException_forUnknownEventType() {
        OutboxRecord record = outboxRecord("order-3", "SomeUnknownEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));

        assertThatThrownBy(() -> publisherService.publishPendingOutboxRecords())
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).delete(any());
    }

    @Test
    void publishPendingOutboxRecords_leavesRecordForRetry_onFailure() {
        OutboxRecord record = outboxRecord("order-fail", "RestaurantApprovedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));

        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(failed);

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(any());
    }

    /**
     * The InterruptedException catch block does something real (Thread.currentThread()
     * .interrupt(), the standard never-swallow-an-interrupt pattern) that nothing else
     * tests. Thread.interrupted() below both asserts and clears the flag, so this test
     * doesn't leak an interrupted thread into whichever test JUnit runs next on it.
     */
    @Test
    void publishPendingOutboxRecords_leavesRecordForRetryAndRestoresInterruptFlag_onInterrupt() throws Exception {
        OutboxRecord record = outboxRecord("order-interrupted", "RestaurantApprovedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));

        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        publisherService.publishPendingOutboxRecords();

        assertThat(Thread.interrupted()).isTrue();
        verify(outboxRepository, never()).delete(any());
    }

    @Test
    void publishPendingOutboxRecords_leavesRecordForRetry_onTimeout() {
        OutboxPublisherService shortTimeoutService = new OutboxPublisherService(
                outboxRepository, kafkaTemplate, "restaurant-approved-topic", "restaurant-rejected-topic", 10, 50L);
        OutboxRecord record = outboxRecord("order-slow", "RestaurantApprovedEvent");
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(record));
        // Never completes - proves the explicit get(timeout) bound fires rather than blocking
        // indefinitely on Kafka's own delivery.timeout.ms default.
        when(kafkaTemplate.send(any(Message.class))).thenReturn(new CompletableFuture<>());

        shortTimeoutService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(any());
    }
}
