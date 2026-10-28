package com.example.saga.gateway.dto;

public record WebTokenResponse(
        String token,
        String type,
        long expiresIn
) {}
