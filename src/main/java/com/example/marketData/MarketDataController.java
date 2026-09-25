package com.example.marketData;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * Returns the latest stored market data for all configured tickers.
     *
     * This does not call Alpaca directly.
     * The scheduled MarketDataService is responsible for keeping
     * the database up to date.
     */
    @GetMapping("/market-data/prices")
    public List<Map<String, Object>> getLatestPrices() {
        return marketDataService.getLatestPrices();
    }

    /**
     * Manually triggers a market-data refresh and then returns
     * the latest stored quotes.
     *
     * The service performs one batched Alpaca request for the
     * symbols that require refreshing.
     */
    @GetMapping("/market-data/refresh")
    public List<Map<String, Object>> refreshMarketData() {
        marketDataService.refreshMarketData();
        return marketDataService.getLatestPrices();
    }

    /**
     * Returns all tickers/assets currently stored in the
     * instruments table.
     */
    @GetMapping("/market-data/tickers")
    public List<String> getTickers() {
        return marketDataService.getTickers();
    }

    /**
     * Returns the status of the most recent market-data refresh.
     */
    @GetMapping("/market-data/refresh/status")
    public MarketDataService.RefreshStatus getRefreshStatus() {
        return marketDataService.getRefreshStatus();
    }

    /**
     * Returns the latest stored quote for one ticker.
     *
     * Example:
     *
     * GET /api/trading/market-data/prices/AAPL
     *
     * GET /api/trading/market-data/prices/BTC/USD
     */
    @GetMapping("/market-data/prices/{ticker}")
    public List<Map<String, Object>> getLatestPrice(
            @PathVariable String ticker) {

        return marketDataService.getLatestPrice(
                ticker.toUpperCase());
    }

    /**
     * Returns historical market data for one ticker.
     *
     * If no dates are supplied, the previous 30 days are returned.
     *
     * Example:
     *
     * GET /api/trading/market-data/prices/AAPL/history
     *
     * GET /api/trading/market-data/prices/AAPL/history
     *     ?from=2026-09-01T00:00:00Z
     *     &to=2026-09-17T23:59:59Z
     */
    @GetMapping("/market-data/prices/{ticker}/history")
    public List<Map<String, Object>> getPriceHistory(
            @PathVariable String ticker,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OffsetDateTime historyStart =
                from == null
                        ? now.minus(Duration.ofDays(30))
                        : from;

        OffsetDateTime historyEnd =
                to == null
                        ? now
                        : to;

        if (historyStart.isAfter(historyEnd)) {
            throw new IllegalArgumentException(
                    "from must be before or equal to to");
        }

        return marketDataService.getPriceHistory(
                ticker.toUpperCase(),
                historyStart,
                historyEnd);
    }
}
