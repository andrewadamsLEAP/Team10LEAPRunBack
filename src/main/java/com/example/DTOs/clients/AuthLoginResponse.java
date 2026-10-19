package com.example.DTOs.clients;

/**
 * Response returned by the unified authentication endpoint.
 */
public record AuthLoginResponse(
        Long userId,
        String username,
        String role,
        String token,
        String message
) {}