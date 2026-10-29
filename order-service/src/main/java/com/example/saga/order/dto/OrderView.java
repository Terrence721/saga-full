package com.example.saga.order.dto;

import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read model for Order responses/streaming (command/read separation): exposes only
 * the fields clients need and keeps write-only/internal fields (card details)
 * out of responses.
 */
public record OrderView(
        UUID id,
        UUID customerId,
        String tableNumber,
        String customerName,
        BigDecimal totalAmount,
        String itemCode,
        int quantity,
        OrderStatus status
) {
    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getCustomerId(),
                order.getTableNumber(),
                order.getCustomerName(),
                order.getTotalAmount(),
                order.getItemCode(),
                order.getQuantity(),
                order.getStatus());
    }
}