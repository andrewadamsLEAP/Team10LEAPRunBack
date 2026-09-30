package com.example.dtos.clients;

public record ClientReporterView(
        Long clientId,
        String username,
        String email
) {}