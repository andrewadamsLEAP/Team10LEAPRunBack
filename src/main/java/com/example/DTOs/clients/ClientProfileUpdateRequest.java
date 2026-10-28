package com.example.DTOs.clients;

import jakarta.validation.constraints.Size;

/**
 * DTO for handling client profile update requests.
 * ClientProfileUpdateRequest
 * Each non-blank field is optional; only supplied fields are updated.
 */

public record ClientProfileUpdateRequest(
        @Size(min = 3, max = 100)
        String username,

        @Size(min = 1, max = 100)
        String firstName,

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