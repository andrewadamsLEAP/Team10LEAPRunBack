package com.example.DTOs.instruments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InstrumentDetail(
        String ticker,
        String displayName,
        String assetType,
        BigDecimal previousClose,
        BigDecimal open,
        Integer volume,
        BigDecimal averageVolume,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal bidSize,
        BigDecimal askSize,
        OffsetDateTime quoteTimestamp,
        OffsetDateTime recordedAt,
        BigDecimal changePercent
) {
}