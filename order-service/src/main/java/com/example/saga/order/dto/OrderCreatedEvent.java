package com.example.saga.order.dto;

import com.example.saga.order.domain.OrderStatus;
import com.example.saga.order.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        String itemCode,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String cardLast4,
        String cardBrand
) {
    public OrderCreatedEvent(UUID orderId, UUID customerId, String itemCode, int quantity,
            BigDecimal totalAmount, OrderStatus status) {
        this(orderId, customerId, itemCode, quantity, totalAmount, status, PaymentMethod.CARD, null, null);
    }
}
