package com.example.saga.order.repository;

import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByIdAndStatus(UUID id, OrderStatus status);
}
