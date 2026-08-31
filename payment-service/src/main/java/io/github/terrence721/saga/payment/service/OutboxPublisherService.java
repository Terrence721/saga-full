package io.github.terrence721.saga.payment.service;

import io.github.terrence721.saga.payment.domain.OutboxRecord;
import io.github.terrence721.saga.payment.repository.OutboxRepository;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OutboxPublisherService {

    private static final String HEADER_EVENT_TYPE = "eventType";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;
    private final long sendTimeoutMs;

    public OutboxPublisherService(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.topic:payment-processed-topic}") String topic,
            @Value("${app.outbox.batch-size:10}") int batchSize,
            @Value("${app.outbox.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
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
        @SuppressWarnings("null") // payload is a NOT NULL DB column; never null once loaded.
        String payload = record.getPayload();
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, record.getAggregateId())
                .setHeader(HEADER_EVENT_TYPE, record.getEventType())
                .build();

        try {
            // Bounded explicitly rather than left to Kafka's own delivery.timeout.ms default
            // (120s, undocumented in this code) - this method is @Transactional, holding the
            // outbox row's PESSIMISTIC_WRITE lock open for as long as this call blocks, so an
            // unbounded wait here means a degraded broker can hold a DB connection and lock
            // open per record for the full 120s, times up to batchSize records in the worst
            // case. A timeout here can leave a record whose send actually succeeds server-side
            // for retry next poll, risking a duplicate publish - already an accepted outcome in
            // this at-least-once system, guarded by consumer-side idempotency checks.
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
