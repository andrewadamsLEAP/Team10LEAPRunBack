package com.example.DTOs.clients;

/**
 * DTO for representing client reporter information.
 * ClientReporterView
 * @param clientId
 * @param username
 * @param email
 */

public record ClientReporterView(
        Long clientId,
        String username,
        String email
) {}