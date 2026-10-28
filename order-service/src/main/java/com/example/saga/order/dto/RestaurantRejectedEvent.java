package com.example.saga.order.dto;

import java.util.UUID;

public record RestaurantRejectedEvent(
        UUID orderId,
        UUID customerId,
        String reason
) {}
