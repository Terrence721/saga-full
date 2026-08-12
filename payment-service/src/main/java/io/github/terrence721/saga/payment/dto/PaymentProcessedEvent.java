package io.github.terrence721.saga.payment.dto;

import io.github.terrence721.saga.payment.domain.PaymentStatus;

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
