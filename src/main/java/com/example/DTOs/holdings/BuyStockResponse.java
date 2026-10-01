package com.example.DTOs.holdings;

public record BuyStockResponse(
    Long clientId,
    String ticker,
    Integer newQuantity,
    String message
) {}
