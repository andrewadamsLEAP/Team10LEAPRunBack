package com.example.DTOs.holdings;

public record SellStockResponse(
    Long clientId,
    String ticker,
    Integer newQuantity,
    String message
) {}
