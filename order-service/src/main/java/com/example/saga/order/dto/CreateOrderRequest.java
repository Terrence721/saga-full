package com.example.saga.order.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.example.saga.order.domain.PaymentMethod;
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
        int quantity,

        @Size(max = 50, message = "Table number must be at most 50 characters")
        String tableNumber,

        @Size(max = 100, message = "Customer name must be at most 100 characters")
        String customerName,

        PaymentMethod paymentMethod,

        @Size(max = 20, message = "Card brand must be at most 20 characters")
        String cardBrand,

        @jakarta.validation.constraints.Pattern(regexp = "\\d{4}", message = "Card last4 must be 4 digits")
        String cardLast4
) {
    public CreateOrderRequest(UUID customerId, BigDecimal totalAmount, String itemCode, int quantity,
            String tableNumber, String customerName) {
        this(customerId, totalAmount, itemCode, quantity, tableNumber, customerName, PaymentMethod.CASH, null, null);
    }

    public CreateOrderRequest(UUID customerId, String itemCode, int quantity, BigDecimal totalAmount) {
        this(customerId, totalAmount, itemCode, quantity, null, null, PaymentMethod.CASH, null, null);
    }

    public CreateOrderRequest(UUID customerId, String itemCode, BigDecimal totalAmount, int quantity) {
        this(customerId, totalAmount, itemCode, quantity, null, null, PaymentMethod.CASH, null, null);
    }
}
