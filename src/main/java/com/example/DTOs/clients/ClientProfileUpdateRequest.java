package com.example.DTOs.clients;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for handling client profile update requests.
 * ClientProfileUpdateRequest
 * @param username
 * ^^^ Cannot be blank and has a min size of 3 and a max size of 100.
 * @param firstName
 * ^^^^^^^^^ Cannot be blank and has a min size of 1 and a max size of 100.
 * @param lastName
 * ^^^^^^^^^ Cannot be blank and has a min size of 1 and a max size of 100.
 */

public record ClientProfileUpdateRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        String username,
        
        @NotBlank
        @Size(min = 1, max = 100)
        String firstName,
        
        @NotBlank
        @Size(min = 1, max = 100)
        String lastName
) {
    public boolean hasUpdates() {
        return hasText(username) || hasText(firstName) || hasText(lastName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}