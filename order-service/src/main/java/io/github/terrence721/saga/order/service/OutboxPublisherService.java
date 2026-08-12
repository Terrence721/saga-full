package io.github.terrence721.saga.order.service;

import io.github.terrence721.saga.order.domain.OutboxRecord;
import io.github.terrence721.saga.order.repository.OutboxRepository;
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

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisherService(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.topic:order-created-topic}") String topic,
            @Value("${app.outbox.batch-size:10}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
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
}
