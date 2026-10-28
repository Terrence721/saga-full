package com.example.saga.order.dto;

import java.util.UUID;

public record RestaurantApprovedEvent(
        UUID orderId,
        UUID customerId,
        UUID ticketId
) {}
