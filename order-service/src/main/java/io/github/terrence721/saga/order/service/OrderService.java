package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import io.github.terrence721.saga.order.domain.OutboxRecord;
import io.github.terrence721.saga.order.dto.CreateOrderRequest;
import io.github.terrence721.saga.order.dto.OrderCreatedEvent;
import io.github.terrence721.saga.order.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.order.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.order.exception.OrderNotFoundException;
import io.github.terrence721.saga.order.repository.OrderRepository;
import io.github.terrence721.saga.order.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    // Multicast, not a per-order-id map: v1 is a one-terminal cashier flow, not many
    // concurrent orders, so one shared bus filtered per-subscriber by order ID is simpler
    // and needs no eviction logic. directBestEffort(), not onBackpressureBuffer(): a
    // backpressure buffer queues emissions made while zero subscribers are connected and
    // replays the whole backlog to the first subscriber that arrives - verified for real
    // against the running stack that this produces a genuinely wrong client-visible symptom
    // (a client connecting after an order already reached SUCCESS saw its stream replay a
    // stale PENDING before SUCCESS again, even though the connection's own current-state
    // snapshot already correctly showed SUCCESS). directBestEffort() delivers only to
    // subscribers connected at emission time and drops the value otherwise - the correct
    // semantics here, since streamOrderUpdates() callers already prepend current state.
    private final Sinks.Many<Order> orderUpdates = Sinks.many().multicast().directBestEffort();

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("null") // Spring Data's save() never returns null; it throws on failure instead.
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.customerId())
                .totalAmount(request.totalAmount())
                .itemCode(request.itemCode())
                .quantity(request.quantity())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} saved with status {}", savedOrder.getId(), savedOrder.getStatus());

        OutboxRecord outboxRecord = buildOutboxRecord(savedOrder);
        outboxRepository.save(outboxRecord);
        emitOrderUpdate(savedOrder);

        return savedOrder;
    }

    public Order getOrder(UUID orderId) {
        return findOrder(orderId);
    }

    // Live-only: no current-state snapshot. Callers building a client-facing stream (see
    // OrderController's /stream endpoint) are expected to prepend getOrder(orderId)'s result
    // themselves, so a subscriber always gets current state immediately, then live pushes.
    public Flux<Order> streamOrderUpdates(UUID orderId) {
        return orderUpdates.asFlux().filter(order -> order.getId().equals(orderId));
    }

    private void emitOrderUpdate(Order order) {
        Sinks.EmitResult result = orderUpdates.tryEmitNext(order);
        if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            // Expected, common outcome with directBestEffort(): most orders won't have an
            // active /stream client at the exact moment their status changes - the client's
            // own connection always gets current state from its snapshot regardless.
            log.debug("No active subscriber for order {} update; skipping live push", order.getId());
        } else if (result.isFailure()) {
            log.warn("Failed to emit order update for order {}: {}", order.getId(), result);
        }
    }

    @SuppressWarnings("null") // orderId() is always a real, non-null UUID from a real event.
    @Transactional
    public void confirmOrder(RestaurantApprovedEvent event) {
        if (orderRepository.existsByIdAndStatus(event.orderId(), OrderStatus.SUCCESS)) {
            log.debug("Order {} already SUCCESS; skipping duplicate approval event", event.orderId());
            return;
        }
        if (event.ticketId() == null) {
            throw new IllegalArgumentException("ticketId is required for order " + event.orderId());
        }

        Order order = findOrder(event.orderId());
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.debug("Order {} already SUCCESS after load; skipping", event.orderId());
            return;
        }
        validateCustomerMatches(order, event.customerId());

        order.setStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);
        emitOrderUpdate(order);
        log.info("Order {} marked SUCCESS", event.orderId());
    }

    @SuppressWarnings("null") // orderId() is always a real, non-null UUID from a real event.
    @Transactional
    public void cancelOrder(RestaurantRejectedEvent event) {
        if (orderRepository.existsByIdAndStatus(event.orderId(), OrderStatus.CANCELLED)) {
            log.debug("Order {} already CANCELLED; skipping duplicate rejection event", event.orderId());
            return;
        }

        Order order = findOrder(event.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order {} already CANCELLED after load; skipping", event.orderId());
            return;
        }
        validateCustomerMatches(order, event.customerId());

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        emitOrderUpdate(order);
        // reason carries order-service's own itemCode unmodified through payment-service and
        // restaurant-service (both raw string-concatenate it into this field) - client-controlled,
        // content-unrestricted, and needs the same CR/LF sanitizing as OrderController's itemCode
        // logging (see that class) before it's safe to log.
        String sanitizedReason = String.valueOf(event.reason()).replaceAll("[\r\n]", "_");
        log.warn("Order {} cancelled: {}", event.orderId(), sanitizedReason);
    }

    @SuppressWarnings("null") // orderId is always a real, non-null UUID from a real caller.
    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    // customerId crosses the wire on both inbound events but this instance already knows the
    // real value from the order it just loaded - cross-checking it here catches a corrupted or
    // mismatched message on the shared Kafka topic, the same failure mode restaurant-service's
    // own inbound-event validation (PaymentProcessedEvent) already guards against.
    private void validateCustomerMatches(Order order, UUID eventCustomerId) {
        if (!order.getCustomerId().equals(eventCustomerId)) {
            throw new IllegalArgumentException("customerId mismatch for order " + order.getId()
                    + ": order has " + order.getCustomerId() + ", event has " + eventCustomerId);
        }
    }

    @SuppressWarnings("null") // Lombok's generated build() never returns null.
    private OutboxRecord buildOutboxRecord(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getItemCode(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus()
        );

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OrderCreatedEvent for order " + order.getId(), e);
        }

        OutboxRecord outboxRecord = OutboxRecord.builder()
                .aggregateId(order.getId().toString())
                .eventType("OrderCreatedEvent")
                .payload(payload)
                .createdTime(LocalDateTime.now())
                .build();
        return outboxRecord;
    }
}
