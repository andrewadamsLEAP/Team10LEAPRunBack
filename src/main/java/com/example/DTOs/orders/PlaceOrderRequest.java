package com.example.DTOs.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * DTO for placing a new order (BUY or SELL)
 * Example: Client wants to BUY 100 shares of AAPL at $150.00
 * {
 *   "clientId": 1,
 *   "ticker": "AAPL",
 *   "quantity": 100,
 *   "price": 150.00
 * }
 * @param clientId
 * @param ticker
 * @param quantity
 * @param price
 */


public record PlaceOrderRequest(
        @NotNull
        Long clientId,
        
        @NotBlank
        String ticker,
        
        @Positive
        Integer quantity,
        
        @Positive
        BigDecimal price
) {}
