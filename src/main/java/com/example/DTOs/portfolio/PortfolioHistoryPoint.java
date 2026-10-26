package com.example.DTOs.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioHistoryPoint(
        OffsetDateTime timestamp,
        BigDecimal cash,
        BigDecimal invested,
        BigDecimal total,
        boolean complete,
        List<String> missingPrices) {
}