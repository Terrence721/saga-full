package io.github.terrence721.saga.order.dto;

import java.util.UUID;

public record ReservationConfirmedEvent(
        UUID orderId,
        UUID customerId,
        String tableNumber,
        int partySize
) {}
