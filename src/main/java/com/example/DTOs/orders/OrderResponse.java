package com.example.DTOs.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning order details to the CLIENT
 * 
 * This is what the SERVER sends back when a client asks for order details
 * or after they place an order. Note: The Order entity in the database probably 
 * has sensitive fields like executedAt, executedPrice, brokerId, commissionFee, etc.
 * These should NOT be exposed to the client in the DTO!
 * 
 * Example response after placing AAPL order:
 * {
 *   "orderId": 42,
 *   "ticker": "AAPL",
 *   "quantity": 100,
 *   "price": 150.00,
 *   "status": "PENDING",
 *   "createdAt": "2026-09-30T18:30:00"
 * }
 */
public record OrderResponse(
    long orderId,
    String ticker,
    Integer quantity,
    BigDecimal price,
    String status,
    LocalDateTime createdAt
) {}
