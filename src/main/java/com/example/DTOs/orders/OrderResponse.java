package com.example.DTOs.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning order details to the CLIENT
 * @param orderId
 * @param ticker
 * @param quantity
 * @param price
 * @param status
 * @param createdAt
 */


public record OrderResponse(
    long orderId,
    String ticker,
    Integer quantity,
    BigDecimal price,
    String status,
    LocalDateTime createdAt
) {}
