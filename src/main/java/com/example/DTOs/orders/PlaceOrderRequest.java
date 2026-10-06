package com.example.DTOs.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * DTO for placing a new order (BUY or SELL).
 * The clientId is extracted from the JWT token (when integrated) or provided in request body.
 * 
 * Example: Client wants to BUY 100 shares of AAPL
 * {
 *   "clientId": 1,
 *   "ticker": "AAPL",
 *   "quantity": 100,
 *   "price": 150.00
 * }
 * 
 * Notes:
 * - clientId: Customer ID (resolved at login time via username validation, stored in JWT)
 * - ticker: Stock/crypto symbol (case-insensitive)
 * - quantity: Number of shares/units to buy or sell
 * - price: DEPRECATED/IGNORED - server uses current market price (ask for buy, bid for sell)
 * 
 * @param clientId the client ID (validated at login, passed via JWT token)
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
