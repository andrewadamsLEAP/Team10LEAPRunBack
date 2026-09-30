package com.example.DTOs.clients;

import java.math.BigDecimal;

public record ClientProfileView(
        Long clientId,
        String email,
        String username,
        String firstName,
        String lastName,
        BigDecimal cashAmount
) {
}