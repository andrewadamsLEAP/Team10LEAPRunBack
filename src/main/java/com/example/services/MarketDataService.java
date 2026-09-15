package com.example.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.repositories.MarketDataRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

// ------------------------------------
// marketDataService is a Spring service that handles market data operations, including retrieving orders for clients and refreshing stock prices for the DOW 30. It uses JdbcTemplate for database interactions and RestClient to fetch data from the Finnhub API. The service ensures that the database schema is correct and that all necessary instruments are present before performing operations. It also includes a scheduled task to refresh stock prices every 30 seconds.
// -------------------------------------

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    // The 30 stocks from the DOW 30 that we will be refreshing.
    // Our service will be checking this list to ensure that we are refreshing the correct stocks and that they are present in the database.
    private static final List<String> DOW_30 = List.of(
            "MMM", "AXP", "AMGN", "AMZN", "AAPL", "BA", "CAT", "CVX", "CSCO", "KO",
            "DIS", "GS", "HD", "HON", "IBM", "JNJ", "JPM", "MCD", "MRK", "MSFT",
            "NKE", "NVDA", "PG", "CRM", "SHW", "TRV", "UNH", "VZ", "V", "WMT");

    private final JdbcTemplate jdbcTemplate;
    private final MarketDataRepository marketDataRepository;
    private final RestClient finnhubClient;
    private final boolean refreshAll;
    private final String apiKey;
    private final Environment environment;
    private final int maxRetries;
    private final long retryBackoffMs;
    private final boolean skipOutsideMarketHours;
    private OffsetDateTime lastRefreshStartedAt;
    private OffsetDateTime lastRefreshCompletedAt;
    private String lastRefreshFailure;
    private int lastSuccessfulTickerCount;
    private int lastFailedTickerCount;

    // Constructor for TradingService, initializes JdbcTemplate and RestClient with Finnhub base URL and refreshAll flag.
    public MarketDataService(
            JdbcTemplate jdbcTemplate,
            MarketDataRepository marketDataRepository,
            @Value("${finnhub.base-url:https://finnhub.io/api/v1}") String finnhubBaseUrl,
            @Value("${finnhub.refresh-all:false}") boolean refreshAll,
            @Value("${finnhub.api-key:}") String apiKey,
            Environment environment,
                @Value("${finnhub.request-timeout-ms:5000}") long requestTimeoutMs,
                @Value("${finnhub.max-retries:2}") int maxRetries,
                @Value("${finnhub.retry-backoff-ms:250}") long retryBackoffMs,
                @Value("${finnhub.skip-outside-market-hours:false}") boolean skipOutsideMarketHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.marketDataRepository = marketDataRepository;
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(requestTimeoutMs))
                    .build());
            requestFactory.setReadTimeout(Duration.ofMillis(requestTimeoutMs));
            this.finnhubClient = RestClient.builder().baseUrl(finnhubBaseUrl)
                .requestFactory(requestFactory).build();
        this.refreshAll = refreshAll;
        this.apiKey = apiKey;
        this.environment = environment;
            this.maxRetries = Math.max(0, maxRetries);
            this.retryBackoffMs = Math.max(0, retryBackoffMs);
            this.skipOutsideMarketHours = skipOutsideMarketHours;
    }

    // Refresh every 30 seconds to ensure rate limits
    // This is the entry point for the scheduled task that refreshes the prices of the DOW 30 stocks every 30 seconds. It calls the refreshDowPrices method with the configured API key.
    // Before we call the refreshDowPrices method, we check if the application is running in a test profile, if the API key is configured, and if we should skip refreshing outside of U.S. market hours.
    // If any of these conditions are not met, we log a message and skip the refresh.
    @Scheduled(fixedDelayString = "${finnhub.refresh-delay-ms:30000}")
    public void scheduledRefreshDowPrices() {
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Skipping scheduled market-data refresh because FINNHUB_API_KEY is not configured");
            return;
        }

        if (skipOutsideMarketHours && !isUsMarketHours()) {
            logger.info("Skipping scheduled market-data refresh outside U.S. market hours");
            lastRefreshCompletedAt = OffsetDateTime.now(ZoneOffset.UTC);
            return;
        }

        refreshDowPrices();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMarketDataStorage() {
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        // Call the ensure database methods
        ensurePricesSchema();
        ensureDowInstruments();
    }

    // Refresh the prices of the DOW 30 stocks. A stock recorded within the last 30 seconds is skipped.
    public synchronized void refreshDowPrices() {
        lastRefreshStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
        lastRefreshFailure = null;
        lastSuccessfulTickerCount = 0;
        lastFailedTickerCount = 0;

        // Simple API key validation
        if (apiKey == null || apiKey.isBlank()) {
            lastRefreshFailure = "FINNHUB_API_KEY is not configured";
            throw new IllegalStateException("FINNHUB_API_KEY is not configured");
        }

        // Checking the last updated timestamp for each stock and only refreshing those that haven't been updated in the last 30 seconds.
        List<String> tickersToRefresh = refreshAll ? DOW_30 : List.of(DOW_30.get(0));
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(Duration.ofSeconds(30));

        // Beginning of main loop to refresh prices
        for (String ticker : tickersToRefresh) {
            OffsetDateTime lastRecorded = marketDataRepository.findLastRecordedAt(ticker);
            // Skip refreshing if the last recorded timestamp is within the last 30 seconds.
            if (lastRecorded != null && lastRecorded.isAfter(cutoff)) {
                lastSuccessfulTickerCount++;
                continue;
            }

            try {
                // Call Finnhub API to get the latest quote for the stock. If the quote is null or has a non-positive current price, it skips storing that quote.
                FinnhubQuote quote = fetchQuoteWithRetry(ticker);

                if (quote == null || quote.current() == null || quote.current().signum() <= 0) {
                    continue;
                }

                // Insert the quotes into the postgres db
                marketDataRepository.saveQuote(
                        ticker, quote.current(), quote.changeAmount(), quote.percentChange(),
                        quote.previousClose(), quote.open(), quote.high(), quote.low(),
                        quote.quoteTimestamp() == null ? null
                                : Instant.ofEpochSecond(quote.quoteTimestamp()).atOffset(ZoneOffset.UTC));
                lastSuccessfulTickerCount++;
            } catch (RestClientException | IllegalArgumentException exception) {
                lastFailedTickerCount++;
                lastRefreshFailure = exception.getMessage();
                logger.warn("Unable to refresh quote for {}", ticker, exception);
            }
        }

        // Set the last refresh time to now, indicating that the refresh operation has completed.
        lastRefreshCompletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    // A loop to continue trying a fetch on the finnhub api if it failed, up to the maxRetries value. If it fails after maxRetries, it throws an exception.
    private FinnhubQuote fetchQuoteWithRetry(String ticker) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return finnhubClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/quote")
                                .queryParam("symbol", ticker)
                                .queryParam("token", apiKey)
                                .build())
                        .retrieve()
                        .body(FinnhubQuote.class);
            } catch (RestClientException exception) {
                if (attempt == maxRetries) {
                    throw exception;
                }
                try {
                    Thread.sleep(retryBackoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Quote retry was interrupted", interruptedException);
                }
            }
        }
        // Throw execpetion if we hit the max amount of tries and STILL dont get any information
        throw new IllegalStateException("Quote request did not produce a result");
    }

    // Checks to see if market is open, if not we dont ping the Finnhub API to avoid unnecessary calls. This method checks if the current time in the Eastern Time Zone is within U.S. market hours (Monday to Friday, 9:30 AM to 4:00 PM).
    private boolean isUsMarketHours() {
        OffsetDateTime easternNow = OffsetDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = easternNow.getDayOfWeek();
        LocalTime time = easternNow.toLocalTime();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                && !time.isBefore(LocalTime.of(9, 30))
                && time.isBefore(LocalTime.of(16, 0));
    }

    // Returns the status of the latest refresh operation, useful for success or fail monitoring
    // More gets below that will be used to get the latest prices, price history, and tickers from the database.
    public synchronized RefreshStatus getRefreshStatus() {
        return new RefreshStatus(
                lastRefreshStartedAt,
                lastRefreshCompletedAt,
                lastRefreshFailure,
                lastSuccessfulTickerCount,
                lastFailedTickerCount);
    }

    public List<Map<String, Object>> getLatestPrices() {
        List<String> tickersToRead = refreshAll ? DOW_30 : List.of(DOW_30.get(0));
        return marketDataRepository.findLatestPrices(tickersToRead);
    }

    public List<Map<String, Object>> getLatestPrice(String ticker) {
        return marketDataRepository.findLatestPrice(ticker.toUpperCase());
    }

    public List<Map<String, Object>> getPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {
        return marketDataRepository.findPriceHistory(ticker, from, to);
    }

    public List<String> getTickers() {
        return marketDataRepository.findTickers();
    }

    /**Ensurement
     * Below will be the main ensurement methods that will ensure the database, (instruments and prices),
     * are in the correct format to recieve data from the Finnhub API, if its now we will do some changes
     * to ensure that the database is in the correct format to recieve data
     */

        // Ensure the instruments table has all the DOW 30 tickers.
    private void ensureDowInstruments() {
        for (String ticker : DOW_30) {
            jdbcTemplate.update(
                "INSERT INTO instruments (ticker, asset_type) "
                    + "VALUES (?, 'STOCK') ON CONFLICT (ticker) DO NOTHING",
                    ticker);
        }
    }


    // Ensure the prices table has the correct schema. If the "timestamp" column exists, it renames it to "recorded_at". It also adds any missing columns with appropriate data types and default values.
    private void ensurePricesSchema() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'prices' AND column_name = 'timestamp'
                    ) AND NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'prices' AND column_name = 'recorded_at'
                    ) THEN
                        ALTER TABLE prices RENAME COLUMN "timestamp" TO recorded_at;
                    END IF;

                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ NOT NULL DEFAULT now();
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS change_amount NUMERIC(18,4);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS percent_change NUMERIC(18,4);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS previous_close NUMERIC(18,2);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS open NUMERIC(18,2);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS high NUMERIC(18,2);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS low NUMERIC(18,2);
                    ALTER TABLE prices ADD COLUMN IF NOT EXISTS quote_timestamp TIMESTAMPTZ;
                    CREATE INDEX IF NOT EXISTS idx_prices_ticker_recorded_at
                        ON prices (ticker, recorded_at DESC);
                END $$;
                """);
    }

    //Finnhubquote that we are grabbing via the API
    private record FinnhubQuote(
            @JsonProperty("c") BigDecimal current,
            @JsonProperty("d") BigDecimal changeAmount,
            @JsonProperty("dp") BigDecimal percentChange,
            @JsonProperty("h") BigDecimal high,
            @JsonProperty("l") BigDecimal low,
            @JsonProperty("o") BigDecimal open,
            @JsonProperty("pc") BigDecimal previousClose,
            @JsonProperty("t") Long quoteTimestamp) {
    }

            // Refresh status variables
            public record RefreshStatus(
                OffsetDateTime lastStartedAt,
                OffsetDateTime lastCompletedAt,
                String lastFailure,
                int successfulTickerCount,
                int failedTickerCount) {
            }

}
