package com.example.DTOs.clients;

/**
 * DTO for representing client login information.
 * ClientLoginView
 * @param clientId
 * @param username
 * @param password
 */

public record ClientLoginView(
        Long clientId,
        String username,
        String password
) {}