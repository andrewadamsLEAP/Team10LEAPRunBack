package com.example.DTOs.instruments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryPoint(
        String ticker,
        BigDecimal price,
        OffsetDateTime timestamp
) {
}
