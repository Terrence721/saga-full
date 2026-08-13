package io.github.terrence721.saga.gateway.dto;

public record WebTokenResponse(
        String token,
        String type,
        long expiresIn
) {}
