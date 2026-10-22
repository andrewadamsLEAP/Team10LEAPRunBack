package com.example.DTOs.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for viewing a client's order history
 * @param orderId
 * @param clientId
 * @param ticker
 * @param quantity
 * @param price
 * @param orderType
 * @param status
 * @param createdAt
 * @param executedPrice
 * @param executedAt
 * @param cancellationReason
 */
public record OrderHistoryView(
        long orderId,
        Long clientId,
        String clientUsername,
        String ticker,
        Integer quantity,
        BigDecimal price,
        String orderType,
        String status,
        LocalDateTime createdAt,
        BigDecimal executedPrice,
        LocalDateTime executedAt,
        String cancellationReason
) {}
