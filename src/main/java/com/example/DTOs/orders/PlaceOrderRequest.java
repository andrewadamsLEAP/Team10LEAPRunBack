package com.example.DTOs.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

//Someone remove the comments from all these DTO's at some point, I am too lazy right now :o)

/**
 * DTO for placing a new order (BUY or SELL)
 * 
 * HINT: This is what the CLIENT sends when they want to place an order.
 * Think about: What does a trader NEED to specify to place an order?
 *   - Who is placing it? (clientId)
 *   - What are they buying/selling? (ticker)
 *   - How much? (quantity)
 *   - At what price? (price)
 * 
 * TODO: Add the fields above with proper validation annotations
 *   - Use @NotNull for required fields
 *   - Use @Positive for quantity/price (they should be > 0)
 *   - Use @NotBlank for ticker (stock symbols like "AAPL")
 * 
 * Example: Client wants to BUY 100 shares of AAPL at $150.00
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
