package com.example.saga.reservation.dto;

import java.util.UUID;

public record RestaurantRejectedEvent(
        UUID orderId,
        UUID customerId,
        String reason
) {}
