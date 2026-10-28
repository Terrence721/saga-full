package com.example.saga.reservation.dto;

import java.util.UUID;

public record ReservationConfirmedEvent(
        UUID orderId,
        UUID customerId,
        String tableNumber,
        int partySize
) {}
