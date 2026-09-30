package com.example.dtos.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for viewing a client's order history
 * 
 * HINT: Similar to OrderResponse, but this is for listing/viewing past orders
 * A client might want to see all their orders and their final status.
 * 
 * Should include most of the same fields as OrderResponse, but you might
 * want to add fields like:
 *   - executedPrice (only if order was FILLED)
 *   - executedAt (when it actually executed)
 *   - cancellationReason (if CANCELLED)
 * 
 * TODO: Create this record with fields that make sense for viewing order history
 *   Hint: Start with the same fields as OrderResponse
 *   Then think about: what EXTRA info makes sense for historical data?
 *   For a FILLED order, they'd want to know the actual price it filled at.
 *   For a CANCELLED order, they'd want to know why.
 * 
 * Example in a list of historical orders:
 * [
 *   {
 *     "orderId": 42,
 *     "ticker": "AAPL",
 *     "quantity": 100,
 *     "price": 150.00,
 *     "executedPrice": 149.95,  // filled at this price
 *     "status": "FILLED",
 *     "createdAt": "2026-09-15T10:30:00",
 *     "executedAt": "2026-09-15T10:31:00"
 *   },
 *   {
 *     "orderId": 43,
 *     "ticker": "GOOGL",
 *     "quantity": 50,
 *     "price": 140.00,
 *     "status": "CANCELLED",
 *     "createdAt": "2026-09-20T14:00:00",
 *     "cancellationReason": "User cancelled"
 *   }
 * ]
 */
public record OrderHistoryView(
        long orderId,
        String ticker,
        Integer quantity,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        BigDecimal executedPrice,
        LocalDateTime executedAt,
        String cancellationReason
) {}
