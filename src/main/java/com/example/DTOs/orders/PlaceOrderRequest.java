package com.example.DTOs.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for placing a new order (BUY or SELL)
 * The system automatically uses the current live market price.
 * Similar to MarketWatch market simulation - users specify quantity only.
 * 
 * Example: Client wants to BUY 10 shares of AAPL
 * {
 *   "clientId": 1,
 *   "ticker": "AAPL",
 *   "quantity": 10
 * }
 * 
 * The order will be created using:
 * - BUY orders: current market ask price
 * - SELL orders: current market bid price
 * 
 * @param clientId client ID
 * @param ticker ticker symbol
 * @param quantity number of shares/units to buy or sell
 */
public record PlaceOrderRequest(
        @NotNull
        Long clientId,
        
        @NotBlank
        String ticker,
        
        @Positive
        Integer quantity
) {}
