package com.example.DTOs.clients;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for handling change password requests.
 * Contains the current password and the new password.
 * @param currentPassword the current password of the client
 * ^^^^^^ Cannot be blank and has a min size of 8 and a max size of 30.
 * @param newPassword the new password to be set for the client
 * ^^^^^^^ Cannot be blank and has a min size of 8 and a max size of 30.
 */

public record ChangePasswordRequest(
        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 30)
        String currentPassword,
        
        @NotBlank
        @Size(min = 8, max = 30)
        String newPassword
) {}