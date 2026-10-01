package com.example.DTOs.holdings;

public record HoldingResponse(
        Long clientId,
        String ticker,
        Integer quantity
) {}
