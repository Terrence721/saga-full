package io.github.terrence721.saga.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Total amount is required")
        @Positive(message = "Total amount must be > 0")
        BigDecimal totalAmount,

        @NotBlank(message = "Item code is required")
        String itemCode,

        @Positive(message = "Quantity must be > 0")
        int quantity
) {}
