package io.github.terrence721.saga.restaurant.dto;

import java.util.UUID;

public sealed interface RestaurantEvent permits RestaurantApprovedEvent, RestaurantRejectedEvent {

    UUID orderId();

    UUID customerId();
}
