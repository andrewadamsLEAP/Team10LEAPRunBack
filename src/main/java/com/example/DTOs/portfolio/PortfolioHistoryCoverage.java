package com.example.DTOs.portfolio;

import java.time.OffsetDateTime;
import java.util.List;

public record PortfolioHistoryCoverage(
        boolean complete,
        OffsetDateTime earliestReconstructibleAt,
        List<String> issues) {
}