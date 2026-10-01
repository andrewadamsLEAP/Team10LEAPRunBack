package com.example.DTOs.clients;

/**
 * DTO for representing client login information.
 * LoginView
 * @param userId
 * @param username
 * @param password
 */

public record LoginView(
        Long   userId,
        String username,
        String password
) {}