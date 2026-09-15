package com.example.controllers;

import com.example.services.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading")
public class MarketDataController {

    // The MarketDataService is injected into the controller to handle the business logic related to market data.
    // These are api endpoints that the frontend can call to get market data, refresh prices, and get the status of the refresh operation.
    // Pretty sweet B)
    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/dow30/prices")
    public List<Map<String, Object>> refreshDowPrices() {
        marketDataService.refreshDowPrices();
        return marketDataService.getLatestPrices();
    }

    @GetMapping("/market-data/tickers")
    public List<String> getTickers() {
        return marketDataService.getTickers();
    }

    @GetMapping("/market-data/refresh/status")
    public MarketDataService.RefreshStatus getRefreshStatus() {
        return marketDataService.getRefreshStatus();
    }

    @GetMapping("/market-data/prices/{ticker}/history")
    public List<Map<String, Object>> getPriceHistory(
            @PathVariable String ticker,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime historyStart = from == null ? now.minus(Duration.ofDays(30)) : from;
        OffsetDateTime historyEnd = to == null ? now : to;
        if (historyStart.isAfter(historyEnd)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        return marketDataService.getPriceHistory(ticker.toUpperCase(), historyStart, historyEnd);
    }

    @GetMapping("/market-data/prices/{ticker}")
    public List<Map<String, Object>> getLatestPrice(@PathVariable String ticker) {
        return marketDataService.getLatestPrice(ticker);
    }
}
