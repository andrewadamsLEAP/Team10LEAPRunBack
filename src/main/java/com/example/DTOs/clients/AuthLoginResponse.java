package com.example.DTOs.clients;

public record AuthLoginResponse(
        Long userId,
        String username,
        String role,
        String token,
        String message) {
}
