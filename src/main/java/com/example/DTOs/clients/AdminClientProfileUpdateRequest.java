package com.example.DTOs.clients;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Optional client profile fields that an employee admin may update. */
public record AdminClientProfileUpdateRequest(
        @Size(min = 3, max = 100)
        String username,

        @Size(min = 1, max = 100)
        String firstName,

        @Size(min = 1, max = 100)
        String lastName,

        @Email
        @Size(max = 255)
        String email
) {
    public boolean hasUpdates() {
        return hasText(username) || hasText(firstName) || hasText(lastName) || hasText(email);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}