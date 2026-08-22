package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.domain.InventoryStatus;
import io.github.terrence721.saga.restaurant.domain.OutboxRecord;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicket;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicketStatus;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.restaurant.dto.PaymentStatus;
import io.github.terrence721.saga.restaurant.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.restaurant.dto.RestaurantEvent;
import io.github.terrence721.saga.restaurant.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.restaurant.repository.OutboxRepository;
import io.github.terrence721.saga.restaurant.repository.RestaurantTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantTicketRepository ticketRepository;
    private final RestaurantInventoryService inventoryService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public RestaurantService(
            RestaurantTicketRepository ticketRepository,
            RestaurantInventoryService inventoryService,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.inventoryService = inventoryService;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processRestaurantStep(PaymentProcessedEvent event) {
        if (ticketRepository.existsByOrderId(event.orderId())) {
            log.debug("Ticket already exists for order {}; skipping duplicate event", event.orderId());
            return;
        }

        validate(event);

        if (event.status() != PaymentStatus.APPROVED) {
            createAndSaveTicket(event.orderId(), RestaurantTicketStatus.REJECTED);
            RestaurantRejectedEvent rejectedEvent = new RestaurantRejectedEvent(
                    event.orderId(), event.customerId(), "Payment failed for order: " + event.orderId());
            saveRestaurantTicketOutbox(rejectedEvent, "RestaurantRejectedEvent");
            return;
        }

        InventoryStatus inventoryStatus = inventoryService.verifyAndDeductStock(event.itemCode(), event.quantity());

        switch (inventoryStatus) {
            case ALLOCATED -> {
                RestaurantTicket ticket = createAndSaveTicket(event.orderId(), RestaurantTicketStatus.PREPARING);
                RestaurantApprovedEvent approvedEvent = new RestaurantApprovedEvent(
                        event.orderId(), event.customerId(), ticket.getId());
                saveRestaurantTicketOutbox(approvedEvent, "RestaurantApprovedEvent");
            }
            case INSUFFICIENT_STOCK -> {
                createAndSaveTicket(event.orderId(), RestaurantTicketStatus.REJECTED);
                RestaurantRejectedEvent rejectedEvent = new RestaurantRejectedEvent(
                        event.orderId(), event.customerId(), "Out of stock for item: " + event.itemCode());
                saveRestaurantTicketOutbox(rejectedEvent, "RestaurantRejectedEvent");
            }
            case ITEM_NOT_FOUND -> {
                createAndSaveTicket(event.orderId(), RestaurantTicketStatus.REJECTED);
                RestaurantRejectedEvent rejectedEvent = new RestaurantRejectedEvent(
                        event.orderId(), event.customerId(), "Invalid item code: " + event.itemCode());
                saveRestaurantTicketOutbox(rejectedEvent, "RestaurantRejectedEvent");
            }
        }
    }

    private void validate(PaymentProcessedEvent event) {
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (event.customerId() == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (event.itemCode() == null) {
            throw new IllegalArgumentException("itemCode is required");
        }
        if (event.status() == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (event.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

    private RestaurantTicket createAndSaveTicket(UUID orderId, RestaurantTicketStatus status) {
        return ticketRepository.save(RestaurantTicket.builder()
                .orderId(orderId)
                .status(status)
                .build());
    }

    private void saveRestaurantTicketOutbox(RestaurantEvent event, String eventType) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + eventType + " for order " + event.orderId(), e);
        }

        OutboxRecord outbox = OutboxRecord.builder()
                .aggregateId(event.orderId().toString())
                .eventType(eventType)
                .payload(payload)
                .createdTime(LocalDateTime.now())
                .build();

        outboxRepository.save(outbox);
        log.info("Saved outbox record for order {} ({})", event.orderId(), eventType);
    }
}
