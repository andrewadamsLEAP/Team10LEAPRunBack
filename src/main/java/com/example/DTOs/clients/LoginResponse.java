package com.example.DTOs.clients;

/**
 * DTO for representing login response information.
 * LoginResponse
 * @param clientId
 * @param username
 * @param token
 * @param message
 */

// TODO:
// eventually the token will store things like clientId and username I just dont know how that works
public record LoginResponse( 
        Long clientId,
        String username,
        String token,
        String message
) {
}