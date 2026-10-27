package io.github.terrence721.saga.reservation.dto;

import java.util.UUID;

public record ReservationRejectedEvent(
        UUID orderId,
        UUID customerId,
        String reason
) {}
