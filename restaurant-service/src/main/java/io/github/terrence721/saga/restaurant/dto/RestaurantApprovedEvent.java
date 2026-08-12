package io.github.terrence721.saga.restaurant.dto;

import java.util.UUID;

public record RestaurantApprovedEvent(
        UUID orderId,
        UUID customerId,
        UUID ticketId
) implements RestaurantEvent {}
