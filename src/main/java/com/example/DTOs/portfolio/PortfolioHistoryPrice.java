package com.example.DTOs.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioHistoryPrice(
        OffsetDateTime valuationTimestamp,
        String ticker,
        BigDecimal bidPrice,
        OffsetDateTime priceTimestamp) {
}