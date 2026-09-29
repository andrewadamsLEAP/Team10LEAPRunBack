package com.example.DTOs.clients;

public record LoginResponse(
        Long clientId,
        String username,
        String token,
        String message
) {
}