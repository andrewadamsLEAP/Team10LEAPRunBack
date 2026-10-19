package com.example.DTOs.clients;

/**
 * DTO for representing login response information.
 * LoginResponse
 * @param userId
 * @param username
 * @param token
 * @param message
 */

public record LoginResponse( 
        Long userId,
        String username,
        String token,
        String message
) {
}