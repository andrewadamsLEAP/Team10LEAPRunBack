package com.example.DTOs.clients;

import java.math.BigDecimal;

/**
 * DTO for representing client profile information.
 * ClientProfileView
 * @param clientId
 * @param email
 * @param username
 * @param firstName
 * @param lastName
 * @param cashAmount
 */

public record ClientProfileView(
        Long clientId,
        String email,
        String username,
        String firstName,
        String lastName,
        BigDecimal cashAmount
) {
}