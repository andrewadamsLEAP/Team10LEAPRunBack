package com.example.DTOs.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * DTO for placing a new order (BUY or SELL)
 * Example: Client wants to BUY 100 shares of AAPL
 * {
 *   "clientId": 1,
 *   "ticker": "AAPL",
 *   "quantity": 100,
 *   "price": 150.00  // IGNORED - order will use current market price (ask for buy, bid for sell)
 * }
 * @param clientId client ID
 * @param ticker ticker symbol
 * @param quantity number of shares/units
 * @param price DEPRECATED/IGNORED - order will use current market price from database
 */


public record PlaceOrderRequest(
        @NotNull
        Long clientId,
        
        @NotBlank
        String ticker,
        
        @Positive
        Integer quantity,
        
        @Positive
        BigDecimal price  // Kept for backward compatibility but ignored - server uses current market price
) {}
