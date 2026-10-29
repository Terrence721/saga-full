package com.example.saga.payment.dto;

import com.example.saga.payment.domain.PaymentMethod;

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
