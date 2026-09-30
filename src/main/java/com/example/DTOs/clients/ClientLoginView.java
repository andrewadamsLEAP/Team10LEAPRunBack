package com.example.dtos.clients;

public record ClientLoginView(
        Long clientId,
        String username,
        String password
) {}