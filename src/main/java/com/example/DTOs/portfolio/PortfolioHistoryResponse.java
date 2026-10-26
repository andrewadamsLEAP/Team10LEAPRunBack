package com.example.DTOs.portfolio;

import java.util.List;

public record PortfolioHistoryResponse(
        String currency,
        PortfolioHistoryCoverage coverage,
        List<PortfolioHistoryPoint> points) {
}