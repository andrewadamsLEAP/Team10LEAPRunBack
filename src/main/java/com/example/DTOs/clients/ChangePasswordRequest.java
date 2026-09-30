package com.example.dtos.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(min = 8, max = 30)
        String currentPassword,
        
        @NotBlank
        @Size(min = 8, max = 30)
        String newPassword
) {}