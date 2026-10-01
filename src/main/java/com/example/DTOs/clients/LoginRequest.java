package com.example.DTOs.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for representing login request information.
 * LoginRequest
 * @param username
 * @param password
 */

public record LoginRequest(
        @NotBlank
        @Size(max = 100)
        String username,
        
        @NotBlank
        @Size(min = 8, max = 30)
        String password
) {}