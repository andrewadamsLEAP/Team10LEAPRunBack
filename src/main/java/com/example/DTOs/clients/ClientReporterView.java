package com.example.DTOs.clients;

public record ClientReporterView(
        Long clientId,
        String username,
        String email
) {}