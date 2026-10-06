package com.example.controllers;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.services.HoldingsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/holdings")
public class HoldingsController {
    private final HoldingsService holdingsService;

    public HoldingsController(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
    }

    //Test endpoint
    // Example: GET /api/v1/holdings/test
    @GetMapping("/test")
    public String test() {
        return "Test endpoint works!";
    }

    //Test the service layer
    //Example: GET /api/holdings/service-test
    @GetMapping("/service-test")
    public String serviceTest() {
        return holdingsService.test();
    }

    //Get specific holding (entire holding) for a client and ticker
    // Example: GET /api/v1/holdings/1/AAPL
    @GetMapping("/{clientId}/{ticker}")
    public HoldingResponse getHolding(
            @PathVariable Long clientId,
            @PathVariable String ticker) {
        return holdingsService.getHolding(clientId, ticker);
    }

   
    // Get all holdings for a specific client
    // Example: GET /api/v1/holdings/client/1
    @GetMapping("/client/{clientId}")
    public java.util.List<HoldingResponse> getClientHoldings(
            @PathVariable Long clientId) {
        return holdingsService.getAllClientHoldings(clientId);
    }

    // Get only quantity of a specific ticker for a client
    // Example: GET /api/v1/holdings/quantity?clientId=1&ticker=AAPL
    @GetMapping("/quantity")
    public QuantityResponse getQuantity(
            @RequestParam Long clientId,
            @RequestParam String ticker) {
        return holdingsService.getQuantity(clientId, ticker);
    }

}
