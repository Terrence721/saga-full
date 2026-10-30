package com.example.saga.reservation.service;

import com.example.saga.reservation.domain.OutboxRecord;
import com.example.saga.reservation.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OutboxPublisherService {

    private static final String HEADER_EVENT_TYPE = "eventType";
    private static final String EVENT_TYPE_CONFIRMED = "ReservationConfirmedEvent";
    private static final String EVENT_TYPE_REJECTED = "ReservationRejectedEvent";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String confirmedTopic;
    private final String rejectedTopic;
    private final int batchSize;
    private final long sendTimeoutMs;
    private final Map<String, String> topicByEventType;

    public OutboxPublisherService(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.confirmed-topic:reservation-confirmed-topic}") String confirmedTopic,
            @Value("${app.outbox.rejected-topic:reservation-rejected-topic}") String rejectedTopic,
            @Value("${app.outbox.batch-size:10}") int batchSize,
            @Value("${app.outbox.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.confirmedTopic = confirmedTopic;
        this.rejectedTopic = rejectedTopic;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
        this.topicByEventType = Map.of(
                EVENT_TYPE_CONFIRMED, this.confirmedTopic,
                EVENT_TYPE_REJECTED, this.rejectedTopic);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.polling-delay-ms:500}")
    public void publishPendingOutboxRecords() {
        List<OutboxRecord> batch = outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Publishing outbox batch of size {}", batch.size());
        for (OutboxRecord record : batch) {
            publish(record);
        }
    }

    @SuppressWarnings("null") // payload is a NOT NULL DB column; never null once loaded.
    private void publish(OutboxRecord record) {
        String topic = topicByEventType.get(record.getEventType());
        if (topic == null) {
            log.error("Unknown outbox event type {} for record {}; will retry next poll", record.getEventType(), record.getId());
            return;
        }

        String payload = record.getPayload();
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, record.getAggregateId())
                .setHeader(HEADER_EVENT_TYPE, record.getEventType())
                .build();

        try {
            kafkaTemplate.send(message).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            outboxRepository.delete(record);
            log.info("Published outbox record {} ({}) to {}", record.getId(), record.getEventType(), topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing outbox record {}; will retry next poll", record.getId(), e);
        } catch (ExecutionException e) {
            log.error("Failed to publish outbox record {} ({}); will retry next poll", record.getId(), record.getEventType(), e);
        } catch (TimeoutException e) {
            log.error("Timed out after {}ms publishing outbox record {} ({}); will retry next poll",
                    sendTimeoutMs, record.getId(), record.getEventType(), e);
        }
    }
}
