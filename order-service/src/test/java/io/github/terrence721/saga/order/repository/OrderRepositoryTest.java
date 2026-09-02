package io.github.terrence721.saga.order.repository;

import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void existsByIdAndStatus_returnsTrue_whenStatusMatches() {
        Order newOrder = Order.builder()
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("19.99"))
                .itemCode("ITEM-1")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(newOrder);

        assertThat(orderRepository.existsByIdAndStatus(saved.getId(), OrderStatus.PENDING)).isTrue();
    }

    @Test
    void existsByIdAndStatus_returnsFalse_whenStatusDoesNotMatch() {
        Order newOrder = Order.builder()
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("19.99"))
                .itemCode("ITEM-1")
                .quantity(2)
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(newOrder);

        assertThat(orderRepository.existsByIdAndStatus(saved.getId(), OrderStatus.SUCCESS)).isFalse();
    }

    @Test
    void existsByIdAndStatus_returnsFalse_whenIdDoesNotExist() {
        assertThat(orderRepository.existsByIdAndStatus(UUID.randomUUID(), OrderStatus.PENDING)).isFalse();
    }
}
