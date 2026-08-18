package io.github.terrence721.saga.order.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Total amount is required")
        @Positive(message = "Total amount must be > 0")
        @Digits(integer = 17, fraction = 2, message = "Total amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal totalAmount,

        @NotBlank(message = "Item code is required")
        @Size(max = 255, message = "Item code must be at most 255 characters")
        String itemCode,

        @Positive(message = "Quantity must be > 0")
        int quantity
) {}
