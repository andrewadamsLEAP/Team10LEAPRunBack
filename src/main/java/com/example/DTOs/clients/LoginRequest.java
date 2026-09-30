package com.example.dtos.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = 100)
        String username,
        
        @NotBlank
        @Size(min = 8, max = 30)
        String password
) {}