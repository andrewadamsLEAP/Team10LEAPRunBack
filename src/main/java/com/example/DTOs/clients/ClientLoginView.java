package com.example.DTOs.clients;

public record ClientLoginView(
        Long clientId,
        String username,
        String password
) {}