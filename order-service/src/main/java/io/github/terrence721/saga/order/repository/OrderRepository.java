package io.github.terrence721.saga.order.repository;

import io.github.terrence721.saga.order.domain.Order;
import io.github.terrence721.saga.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByIdAndStatus(UUID id, OrderStatus status);
}
