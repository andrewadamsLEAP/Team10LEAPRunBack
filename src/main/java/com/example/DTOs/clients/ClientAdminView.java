package com.example.DTOs.clients;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;

/**
 * DTO for representing client information in the admin view.
 * ClientAdminView
 * @param clientId
 * @param username
 * @param email
 * @param firstName
 * @param lastName
 * @param cashAmount
 */

public record ClientAdminView(
        Long clientId,
        String username,
        @Email
        String email,
        String firstName,
        String lastName,
        BigDecimal cashAmount
) {}