package com.example.saga.reservation.service;

import com.example.saga.reservation.domain.OutboxRecord;
import com.example.saga.reservation.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OutboxPublisherServiceFailureTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherService publisherService;

    @BeforeEach
    void setUp() {
        publisherService = new OutboxPublisherService(
                outboxRepository, kafkaTemplate, "reservation-confirmed-topic", "reservation-rejected-topic", 10, 10L);
    }

    private OutboxRecord record() {
        return OutboxRecord.builder()
                .id(UUID.randomUUID())
                .aggregateId("order-1")
                .eventType("ReservationConfirmedEvent")
                .payload("{\"orderId\":\"order-1\"}")
                .createdTime(LocalDateTime.now())
                .build();
    }

    @Test
    void publish_handlesExecutionException() {
        OutboxRecord rec = record();
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(rec));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(rec);
    }

    @Test
    void publish_handlesTimeoutException() {
        OutboxRecord rec = record();
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(rec));
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(new CompletableFuture<>());

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(rec);
    }

    @Test
    void publish_handlesInterruptedException() {
        OutboxRecord rec = record();
        when(outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10)))
                .thenReturn(List.of(rec));
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>() {
            @Override
            public SendResult<String, String> get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("interrupted");
            }
        };
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        publisherService.publishPendingOutboxRecords();

        verify(outboxRepository, never()).delete(rec);
        Thread.interrupted(); // clear any leftover interrupt flag
    }
}
