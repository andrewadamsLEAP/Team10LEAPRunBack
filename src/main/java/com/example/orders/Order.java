package com.example.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Order(
        Long orderId,
        Long clientId,
        String ticker,
        OrderType orderType,
        OrderStatus orderStatus,
        int quantity,
        BigDecimal price,
        OffsetDateTime orderDate
) {

    public enum OrderType {
        BUY,
        SELL
    }

    public enum OrderStatus {
        PENDING,
        FULFILLED,
        CANCELLED
    }
}