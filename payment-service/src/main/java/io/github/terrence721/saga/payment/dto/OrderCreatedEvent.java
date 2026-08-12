package io.github.terrence721.saga.payment.dto;

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
