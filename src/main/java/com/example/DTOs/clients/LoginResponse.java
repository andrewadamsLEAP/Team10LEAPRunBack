package com.example.DTOs.clients;

// TODO:
// eventually the token will store things like clientId and username I just dont know how that works
public record LoginResponse( 
        Long clientId,
        String username,
        String token,
        String message
) {
}