package io.github.terrence721.saga.reservation.dto;

import java.util.UUID;

public record RestaurantApprovedEvent(
        UUID orderId,
        UUID customerId,
        UUID ticketId
) {}
