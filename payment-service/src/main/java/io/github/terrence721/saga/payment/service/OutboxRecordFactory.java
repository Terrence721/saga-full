package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.payment.domain.OutboxRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Owns the one piece of policy every outbox row needs regardless of which event
// triggered it: serialize to JSON (wrapping a failure the same way every time) and
// stamp aggregateId/eventType/payload/createdTime. Mapping a domain entity to its
// specific event DTO stays the caller's job - this factory has no opinion on what
// a PaymentProcessedEvent/OrderCreatedEvent/etc. actually contains.
@Component
public class OutboxRecordFactory {

    private final ObjectMapper objectMapper;

    public OutboxRecordFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("null") // Lombok's generated build() never returns null.
    public OutboxRecord create(String aggregateId, String eventType, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType + " for aggregate " + aggregateId, e);
        }

        return OutboxRecord.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .createdTime(LocalDateTime.now())
                .build();
    }
}
