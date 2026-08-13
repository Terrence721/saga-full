package io.github.terrence721.saga.gateway.exception;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
