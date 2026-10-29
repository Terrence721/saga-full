package com.example.saga.order.service;

import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderStatus;
import com.example.saga.order.domain.OutboxRecord;
import com.example.saga.order.domain.PaymentMethod;
import com.example.saga.order.dto.CreateOrderRequest;
import com.example.saga.order.dto.OrderCreatedEvent;
import com.example.saga.order.dto.RestaurantApprovedEvent;
import com.example.saga.order.dto.RestaurantRejectedEvent;
import com.example.saga.order.dto.ReservationConfirmedEvent;
import com.example.saga.order.dto.ReservationRejectedEvent;
import com.example.saga.order.exception.OrderNotFoundException;
import com.example.saga.order.repository.OrderRepository;
import com.example.saga.order.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final OutboxRecordFactory outboxRecordFactory;
    private final OrderUpdatePublisher orderUpdatePublisher;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            OutboxRecordFactory outboxRecordFactory, OrderUpdatePublisher orderUpdatePublisher) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.outboxRecordFactory = outboxRecordFactory;
        this.orderUpdatePublisher = orderUpdatePublisher;
    }

    @SuppressWarnings("null") // Spring Data's save() never returns null; it throws on failure instead.
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String cardNumber = request.cardNumber();
        String derivedLast4 = request.cardLast4();
        if (derivedLast4 == null && cardNumber != null && cardNumber.length() >= 4) {
            derivedLast4 = cardNumber.substring(cardNumber.length() - 4);
        }
        String derivedBrand = request.cardBrand();
        if (derivedBrand == null && cardNumber != null) {
            char first = cardNumber.charAt(0);
            if (first == '4') {
                derivedBrand = "VISA";
            } else if (first == '5') {
                derivedBrand = "MASTERCARD";
            } else if (first == '3') {
                derivedBrand = "AMEX";
            }
        }

        Order order = Order.builder()
                .customerId(request.customerId())
                .totalAmount(request.totalAmount())
                .itemCode(request.itemCode())
                .quantity(request.quantity())
                .tableNumber(request.tableNumber())
                .customerName(request.customerName())
                .paymentMethod(request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod())
                .cardBrand(derivedBrand)
                .cardLast4(derivedLast4)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} saved with status {}", savedOrder.getId(), savedOrder.getStatus());

        OutboxRecord outboxRecord = buildOutboxRecord(savedOrder);
        outboxRepository.save(outboxRecord);
        orderUpdatePublisher.publish(savedOrder);

        return savedOrder;
    }

    public Order getOrder(UUID orderId) {
        return findOrder(orderId);
    }

    @Transactional
    public void confirmOrder(RestaurantApprovedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for approval event");
        }
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
        orderUpdatePublisher.publish(order);
        log.info("Order {} marked SUCCESS", event.orderId());
    }

    @Transactional
    public void cancelOrder(RestaurantRejectedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for rejection event");
        }
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
        orderUpdatePublisher.publish(order);
        // reason carries order-service's own itemCode unmodified through payment-service and
        // restaurant-service (both raw string-concatenate it into this field) - client-controlled,
        // content-unrestricted, and needs the same CR/LF sanitizing as OrderController's itemCode
        // logging (see that class) before it's safe to log.
        String sanitizedReason = String.valueOf(event.reason()).replaceAll("[\r\n]", "_");
        log.warn("Order {} cancelled: {}", event.orderId(), sanitizedReason);
    }


    @Transactional
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for reservation confirmation");
        }
        if (orderRepository.existsByIdAndStatus(event.orderId(), OrderStatus.SUCCESS)) {
            log.debug("Order {} already SUCCESS; skipping duplicate reservation confirmation", event.orderId());
            return;
        }
        Order order = findOrder(event.orderId());
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.debug("Order {} already SUCCESS after load; skipping", event.orderId());
            return;
        }
        validateCustomerMatches(order, event.customerId());

        order.setStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);
        orderUpdatePublisher.publish(order);
        log.info("Order {} marked SUCCESS after reservation confirmed (table {})", event.orderId(), event.tableNumber());
    }

    @Transactional
    public void handleReservationRejected(ReservationRejectedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required for reservation rejection");
        }
        if (orderRepository.existsByIdAndStatus(event.orderId(), OrderStatus.CANCELLED)) {
            log.debug("Order {} already CANCELLED; skipping duplicate reservation rejection", event.orderId());
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
        orderUpdatePublisher.publish(order);

        String sanitizedReason = String.valueOf(event.reason()).replaceAll("[\r\n]", "_");
        log.warn("Order {} cancelled after reservation rejected: {}", event.orderId(), sanitizedReason);
    }

    private Order findOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
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

    private OutboxRecord buildOutboxRecord(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getItemCode(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getCardLast4(),
                order.getCardBrand()
        );
        return outboxRecordFactory.create(order.getId().toString(), "OrderCreatedEvent", event);
    }
}
