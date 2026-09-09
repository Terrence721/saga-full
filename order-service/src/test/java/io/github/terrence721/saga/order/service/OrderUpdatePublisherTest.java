package io.github.terrence721.saga.order.service;

import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderUpdatePublisherTest {

    private final OrderUpdatePublisher publisher = new OrderUpdatePublisher();

    private Order pendingOrder(UUID id) {
        return Order.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("19.99"))
                .itemCode("ITEM-1")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void streamUpdates_receivesAPublishedOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = pendingOrder(orderId);

        StepVerifier.create(publisher.streamUpdates(orderId))
                .then(() -> publisher.publish(order))
                .assertNext(received -> assertThat(received.getId()).isEqualTo(orderId))
                .thenCancel()
                .verify(Duration.ofSeconds(1));
    }

    @Test
    void streamUpdates_filtersOutUpdatesForOtherOrders() {
        UUID orderId = UUID.randomUUID();
        UUID otherOrderId = UUID.randomUUID();
        Order thisOrder = pendingOrder(orderId);
        Order otherOrder = pendingOrder(otherOrderId);

        StepVerifier.create(publisher.streamUpdates(orderId))
                .then(() -> {
                    publisher.publish(otherOrder);
                    publisher.publish(thisOrder);
                })
                .assertNext(received -> assertThat(received.getId()).isEqualTo(orderId))
                .thenCancel()
                .verify(Duration.ofSeconds(1));
    }

    @Test
    void publish_doesNotThrow_whenNoSubscriberIsListening() {
        // directBestEffort() drops the value instead of buffering it for a subscriber
        // that never arrives - the common case for most orders, since a cashier won't
        // have every order's live view open at the exact moment its status changes.
        Order order = pendingOrder(UUID.randomUUID());

        publisher.publish(order);
    }
}
