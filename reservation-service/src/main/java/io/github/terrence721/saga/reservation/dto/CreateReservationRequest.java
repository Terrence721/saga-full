package io.github.terrence721.saga.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateReservationRequest(
        @NotBlank String customerId,
        @NotBlank String tableNumber,
        @Min(1) int partySize,
        @NotNull Instant startTime
) {
}
