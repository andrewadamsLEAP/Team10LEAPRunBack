package com.example.DTOs.clients;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;

public record ClientAdminView(
        Long clientId,
        String username,
        @Email
        String email,
        String firstName,
        String lastName,
        BigDecimal cashAmount
) {}