package com.example.saga.payment.dto;

import com.example.saga.payment.domain.PaymentStatus;

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
