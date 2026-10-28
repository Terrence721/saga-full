package com.example.saga.restaurant.dto;

import java.util.UUID;

public record RestaurantRejectedEvent(
        UUID orderId,
        UUID customerId,
        String reason
) implements RestaurantEvent {}
