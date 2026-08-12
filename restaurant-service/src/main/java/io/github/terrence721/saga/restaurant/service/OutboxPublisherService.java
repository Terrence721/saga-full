package io.github.terrence721.saga.restaurant.service;

import io.github.terrence721.saga.restaurant.domain.OutboxRecord;
import io.github.terrence721.saga.restaurant.repository.OutboxRepository;
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
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class OutboxPublisherService {

    private static final String HEADER_EVENT_TYPE = "eventType";
    private static final String EVENT_TYPE_APPROVED = "RestaurantApprovedEvent";
    private static final String EVENT_TYPE_REJECTED = "RestaurantRejectedEvent";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String approvedTopic;
    private final String rejectedTopic;
    private final int batchSize;

    public OutboxPublisherService(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.approved-topic:restaurant-approved-topic}") String approvedTopic,
            @Value("${app.outbox.rejected-topic:restaurant-rejected-topic}") String rejectedTopic,
            @Value("${app.outbox.batch-size:10}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.approvedTopic = approvedTopic;
        this.rejectedTopic = rejectedTopic;
        this.batchSize = batchSize;
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

    private void publish(OutboxRecord record) {
        String topic = resolveTopic(record.getEventType());

        Message<String> message = MessageBuilder.withPayload(record.getPayload())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, record.getAggregateId())
                .setHeader(HEADER_EVENT_TYPE, record.getEventType())
                .build();

        try {
            kafkaTemplate.send(message).get();
            outboxRepository.delete(record);
            log.info("Published outbox record {} ({}) to {}", record.getId(), record.getEventType(), topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing outbox record {}; will retry next poll", record.getId(), e);
        } catch (ExecutionException e) {
            log.error("Failed to publish outbox record {} ({}); will retry next poll", record.getId(), record.getEventType(), e);
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case EVENT_TYPE_APPROVED -> approvedTopic;
            case EVENT_TYPE_REJECTED -> rejectedTopic;
            default -> throw new IllegalArgumentException("Unknown outbox event type: " + eventType);
        };
    }
}
