package com.example.saga.reservation.dto;

import java.util.UUID;

public record RestaurantApprovedEvent(
        UUID orderId,
        UUID customerId,
        UUID ticketId
) {}
