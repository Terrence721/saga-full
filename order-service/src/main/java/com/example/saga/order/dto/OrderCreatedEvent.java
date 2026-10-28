package com.example.saga.order.dto;

import com.example.saga.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        String itemCode,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status
) {}
