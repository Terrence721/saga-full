package io.github.terrence721.saga.payment.dto;

import java.util.UUID;

public record ReservationRejectedEvent(
        UUID orderId,
        UUID customerId,
        String reason
) {}
