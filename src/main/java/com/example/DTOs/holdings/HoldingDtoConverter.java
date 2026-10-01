package com.example.DTOs.holdings;

import com.example.entities.Holding;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converter service for Holdings entity to DTO conversions
 * Handles transformations between Holding entities and various DTO response objects
 */
@Service
public class HoldingDtoConverter {

    /**
     * Convert Holding entity to HoldingResponse DTO
     */
    public HoldingResponse toHoldingResponse(Holding holding) {
        if (holding == null) {
            return null;
        }
        return new HoldingResponse(
                holding.getClient_Id(),
                holding.getTicker(),
                holding.getQuantity()
        );
    }

    /**
     * Convert list of Holding entities to HoldingResponse DTOs
     */
    public List<HoldingResponse> toHoldingResponses(List<Holding> holdings) {
        return holdings.stream()
                .map(this::toHoldingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Holding entity to QuantityResponse DTO
     */
    public QuantityResponse toQuantityResponse(Integer quantity) {
        return new QuantityResponse(quantity);
    }

    /**
     * Convert Holding data to BuyStockResponse DTO
     */
    public BuyStockResponse toBuyStockResponse(Long clientId, String ticker, Integer newQuantity) {
        return new BuyStockResponse(
                clientId,
                ticker,
                newQuantity,
                "Stock purchased successfully"
        );
    }

    /**
     * Convert Holding data to SellStockResponse DTO
     */
    public SellStockResponse toSellStockResponse(Long clientId, String ticker, Integer newQuantity) {
        return new SellStockResponse(
                clientId,
                ticker,
                newQuantity,
                "Stock sold successfully"
        );
    }
}
