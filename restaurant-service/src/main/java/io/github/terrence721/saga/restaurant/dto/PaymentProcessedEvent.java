package io.github.terrence721.saga.restaurant.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        UUID customerId,
        String itemCode,
        int quantity,
        BigDecimal amount,
        PaymentStatus status
) {}
