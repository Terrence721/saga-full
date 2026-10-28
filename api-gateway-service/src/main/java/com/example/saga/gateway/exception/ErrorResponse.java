package com.example.saga.gateway.exception;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
