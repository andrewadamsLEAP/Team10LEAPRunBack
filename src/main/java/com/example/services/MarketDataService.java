package com.example.services;

/**
 * Springboot Framework Imports
 */


import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.repositories.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {

    // =========================================================
    // Initial Variable Setup
    // =========================================================

    private static final Logger logger =
            LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataRepository marketDataRepository;

    private final RestClient stocksClient;
    private final RestClient cryptoClient;
    private final RestClient forexClient;

    private final boolean refreshAll;
    private final boolean refreshForex;

    private final String apiKey;
    private final String apiSecret;

    private final Environment environment;

    private final int maxRetries;
    private final long retryBackoffMs;

    private final boolean skipOutsideMarketHours;

    private OffsetDateTime lastRefreshStartedAt;
    private OffsetDateTime lastRefreshCompletedAt;
    private String lastRefreshFailure;

    private int lastSuccessfulTickerCount;
    private int lastFailedTickerCount;


    // ==========================================
    // MarketDataService Class 
    // ==========================================
    
    public MarketDataService(
            MarketDataRepository marketDataRepository,

            @Value("${alpaca.stocks-base-url:https://data.alpaca.markets/v2/stocks}")
            String stocksBaseUrl,

            @Value("${alpaca.crypto-base-url:https://data.alpaca.markets/v1beta3/crypto/us}")
            String cryptoBaseUrl,

            @Value("${alpaca.forex-base-url:https://data.alpaca.markets/v1beta3/forex}")
            String forexBaseUrl,

            @Value("${alpaca.refresh-all:true}")
            boolean refreshAll,

            @Value("${alpaca.refresh-forex:false}")
            boolean refreshForex,

            @Value("${alpaca.api-key:}")
            String apiKey,

            @Value("${alpaca.api-secret:}")
            String apiSecret,

            Environment environment,

            @Value("${alpaca.request-timeout-ms:5000}")
            long requestTimeoutMs,

            @Value("${alpaca.max-retries:2}")
            int maxRetries,

            @Value("${alpaca.retry-backoff-ms:2500}")
            long retryBackoffMs,

            @Value("${alpaca.skip-outside-market-hours:false}")
            boolean skipOutsideMarketHours) {

        this.marketDataRepository = marketDataRepository;

        // Building the Http Request Factory
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(
                                        Duration.ofMillis(requestTimeoutMs))
                                .build());

        requestFactory.setReadTimeout(
                Duration.ofMillis(requestTimeoutMs));

        // Stock Rest Client
        this.stocksClient = RestClient.builder()
                .baseUrl(stocksBaseUrl)
                .requestFactory(requestFactory)
                .build();

        // Crypto Rest Client
        this.cryptoClient = RestClient.builder()
                .baseUrl(cryptoBaseUrl)
                .requestFactory(requestFactory)
                .build();

        // Forex Rest Client
        this.forexClient = RestClient.builder()
                .baseUrl(forexBaseUrl)
                .requestFactory(requestFactory)
                .build();

        this.refreshAll = refreshAll;
        this.refreshForex = refreshForex;

        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        this.environment = environment;

        this.maxRetries = Math.max(0, maxRetries);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);

        this.skipOutsideMarketHours =
                skipOutsideMarketHours;
    }

    // Refresh period for calling the API again.
    // 2000 ms is the sweet spot, any time below this doesnt make any changes to api calling time,
    // as Alpaca is limiting us.
    @Scheduled(
            fixedDelayString =
                    "${alpaca.refresh-delay-ms:2000}")

    // ScheduledRefreshMarketData Class

    public void scheduledRefreshMarketData() {

        logger.info(
                "Starting scheduled market-data refresh. " +
                "Alpaca credentials configured: key={}, secret={}",
                apiKey != null && !apiKey.isBlank(),
                apiSecret != null && !apiSecret.isBlank());

        if (environment.acceptsProfiles(
                Profiles.of("test"))) {
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {

            logger.warn(
                    "ALPACA_API_KEY is not configured");

            return;
        }

        if (apiSecret == null || apiSecret.isBlank()) {

            logger.warn(
                    "ALPACA_API_SECRET is not configured");

            return;
        }

        try {

            refreshMarketData();

        } catch (Exception exception) {

            lastRefreshFailure =
                    exception.getMessage();

            logger.error(
                    "Market-data refresh failed",
                    exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMarketDataStorage() {

        // If we are in the test enviornment, stop this method here and dont ensure the schema
        if (environment.acceptsProfiles(
                Profiles.of("test"))) {
            return;
        }

        try {

            ensurePricesSchema();

            logger.info(
                    "Prices schema initialized");

        } catch (Exception exception) {

            logger.error(
                    "Unable to initialize prices schema",
                    exception);
        }
    }

    public synchronized void refreshMarketData() {

        lastRefreshStartedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        lastRefreshFailure = null;

        lastSuccessfulTickerCount = 0;
        lastFailedTickerCount = 0;

        validateCredentials();

        List<Instrument> instruments =
                marketDataRepository.findInstruments();

        logger.info(
                "Found {} instruments",
                instruments == null
                        ? 0
                        : instruments.size());

        logger.info(
                "Instruments: {}",
                instruments);

        if (instruments == null
                || instruments.isEmpty()) {

            logger.info(
                    "No instruments are configured");

            lastRefreshCompletedAt =
                    OffsetDateTime.now(
                            ZoneOffset.UTC);

            return;
        }

        // If resfresh all in the application properties is set to false, just refresh the first stock in the index
        if (!refreshAll) {

            instruments =
                    List.of(instruments.get(0));
        }

        List<Instrument> instrumentsToRefresh =
                new ArrayList<>(instruments);

        List<String> stockSymbols =
                instrumentsToRefresh.stream()
                        .filter(this::isStock)
                        .map(Instrument::getTicker)
                        .toList();

        List<String> cryptoSymbols =
                instrumentsToRefresh.stream()
                        .filter(this::isCrypto)
                        .map(Instrument::getTicker)
                        .toList();

        List<String> forexSymbols =
                instrumentsToRefresh.stream()
                        .filter(this::isForex)
                        .map(Instrument::getTicker)
                        .toList();

        logger.info(
                "Stock symbols: {}",
                stockSymbols);

        logger.info(
                "Crypto symbols: {}",
                cryptoSymbols);

        logger.info(
                "Forex symbols: {}",
                forexSymbols);

        /*
         * STOCK
         *
         * Stock refresh can optionally be restricted
         * to U.S. market hours.
         */
        if (!stockSymbols.isEmpty()) {

            if (skipOutsideMarketHours
                    && !isUsMarketHours()) {

                logger.info(
                        "Skipping stock refresh because " +
                        "U.S. market is closed");

            } else {

                refreshStocks(stockSymbols);
            }
        }

        /*
         * CRYPTO
         *
         * Crypto trades continuously and is therefore
         * not restricted by U.S. stock-market hours.
         */
        if (!cryptoSymbols.isEmpty()) {

            refreshCrypto(cryptoSymbols);
        }

        /*
         * FOREX
         *
         * Forex refresh can be enabled/disabled through:
         *
         * alpaca.refresh-forex=true
         * alpaca.refresh-forex=false
         *
         * It is currently disabled by default.
         */
        if (refreshForex && !forexSymbols.isEmpty()) {

            refreshForex(forexSymbols);

        } else if (!refreshForex
                && !forexSymbols.isEmpty()) {

            logger.info(
                    "Forex refresh is disabled by configuration");
        }

        lastRefreshCompletedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        logger.info(
                "Market-data refresh complete. " +
                "Successful: {}, Failed: {}",
                lastSuccessfulTickerCount,
                lastFailedTickerCount);
    }

    private void refreshStocks(
            List<String> symbols) {

        try {

            String symbolParameter =
                    String.join(",", symbols);

            logger.info(
                    "Requesting stock quotes for {} symbols: {}",
                    symbols.size(),
                    symbols);

            AlpacaQuotesResponse response =
                    executeQuoteRequest(
                            stocksClient,
                            "/quotes/latest",
                            symbolParameter);

            processQuotes(
                    symbols,
                    response);

        } catch (RestClientException
                | IllegalArgumentException
                | IllegalStateException exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh stock quotes",
                    exception);
        }
    }

    private void refreshCrypto(
            List<String> symbols) {

        try {

            /*
             * Database format:
             *
             * BTC-USD
             *
             * Alpaca format:
             *
             * BTC/USD
             */
            String symbolParameter =
                    symbols.stream()
                            .map(this::toAlpacaCryptoSymbol)
                            .reduce(
                                    (a, b) -> a + "," + b)
                            .orElse("");

            logger.info(
                    "Requesting crypto quotes for {} symbols: {}",
                    symbols.size(),
                    symbolParameter);

            AlpacaQuotesResponse response =
                    executeQuoteRequest(
                            cryptoClient,
                            "/latest/quotes",
                            symbolParameter);

            processQuotes(
                    symbols,
                    response);

        } catch (RestClientException
                | IllegalArgumentException
                | IllegalStateException exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh crypto quotes",
                    exception);
        }
    }

    private void refreshForex(
            List<String> symbols) {

        try {

            /*
             * Database format:
             *
             * EURUSD
             *
             * Alpaca format:
             *
             * EUR/USD
             */
            String symbolParameter =
                    symbols.stream()
                            .map(this::toAlpacaForexSymbol)
                            .reduce(
                                    (a, b) -> a + "," + b)
                            .orElse("");

            logger.info(
                    "Requesting forex quotes for {} symbols: {}",
                    symbols.size(),
                    symbolParameter);

            AlpacaQuotesResponse response =
                    executeQuoteRequest(
                            forexClient,
                            "/latest/quotes",
                            symbolParameter);

            processQuotes(
                    symbols,
                    response);

        } catch (RestClientException
                | IllegalArgumentException
                | IllegalStateException exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh forex quotes",
                    exception);
        }
    }

    // =====================================
    // Main class that, after grabbing all the symbols above, calls the api for all of them.
    // Takes the string of all the symbols we are requesting, and the alpacaquotesresponse.
    // =====================================

    private void processQuotes(
            List<String> requestedSymbols,
            AlpacaQuotesResponse response) {

        if (response == null
                || response.quotes() == null) {

            throw new IllegalStateException(
                    "Alpaca returned an empty quotes response");
        }

        logger.info(
                "Alpaca returned {} quotes",
                response.quotes().size());

        for (String ticker : requestedSymbols) {

            AlpacaQuote quote =
                    findQuote(
                            ticker,
                            response);

            if (quote == null) {

                lastFailedTickerCount++;

                logger.warn(
                        "Alpaca returned no quote for {}",
                        ticker);

                continue;
            }

            if (quote.askPrice() == null
                    || quote.askPrice().signum() <= 0) {

                lastFailedTickerCount++;

                logger.warn(
                        "Invalid ask price for {}",
                        ticker);

                continue;
            }

            // If everything goes through fine, save the quote and timestamp
            OffsetDateTime quoteTimestamp =
                    quote.quoteTimestamp() != null
                            ? quote.quoteTimestamp()
                            : OffsetDateTime.now(
                                    ZoneOffset.UTC);

            marketDataRepository.saveQuote(
                    ticker,
                    quote.askPrice(),
                    quote.askSize(),
                    quote.askExchange(),
                    quote.bidPrice(),
                    quote.bidSize(),
                    quote.bidExchange(),
                    quote.tape(),
                    quoteTimestamp);

            lastSuccessfulTickerCount++;

            logger.info(
                    "Saved quote for {}: ask={}, bid={}, quoteTimestamp={}",
                    ticker,
                    quote.askPrice(),
                    quote.bidPrice(),
                    quoteTimestamp);
        }
    }

    private AlpacaQuote findQuote(
            String ticker,
            AlpacaQuotesResponse response) {

        AlpacaQuote quote =
                response.quotes().get(ticker);

        if (quote != null) {
            return quote;
        }

        /*
         * Crypto:
         *
         * BTC-USD <-> BTC/USD
         */
        if (isCryptoTicker(ticker)) {

            quote =
                    response.quotes().get(
                            toAlpacaCryptoSymbol(ticker));

            if (quote != null) {
                return quote;
            }
        }

        /*
         * Forex:
         *
         * EURUSD <-> EUR/USD
         */
        if (isForexTicker(ticker)) {

            quote =
                    response.quotes().get(
                            toAlpacaForexSymbol(ticker));

            if (quote != null) {
                return quote;
            }
        }

        return null;
    }

    private AlpacaQuotesResponse executeQuoteRequest(
            RestClient client,
            String path,
            String symbols) {

        for (int attempt = 0;
             attempt <= maxRetries;
             attempt++) {

            try {

                return client.get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path(path)
                                        .queryParam(
                                                "symbols",
                                                symbols)
                                        .build())
                        .header(
                                "APCA-API-KEY-ID",
                                apiKey)
                        .header(
                                "APCA-API-SECRET-KEY",
                                apiSecret)
                        .retrieve()
                        .body(
                                AlpacaQuotesResponse.class);

            } catch (RestClientException exception) {

                if (attempt == maxRetries) {

                    throw exception;
                }

                logger.warn(
                        "Alpaca request failed. " +
                        "Retrying {}/{}. Error: {}",
                        attempt + 1,
                        maxRetries,
                        exception.getMessage());

                try {

                    Thread.sleep(
                            retryBackoffMs);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Quote retry was interrupted",
                            interruptedException);
                }
            }
        }

        throw new IllegalStateException(
                "Quote request did not produce a result");
    }

    private void validateCredentials() {

        if (apiKey == null
                || apiKey.isBlank()) {

            lastRefreshFailure =
                    "ALPACA_API_KEY is not configured";

            throw new IllegalStateException(
                    "ALPACA_API_KEY is not configured");
        }

        if (apiSecret == null
                || apiSecret.isBlank()) {

            lastRefreshFailure =
                    "ALPACA_API_SECRET is not configured";

            throw new IllegalStateException(
                    "ALPACA_API_SECRET is not configured");
        }
    }


    // Is ticker type X checks below
    // Ensures tickers are not missing and are equal to their appropriate types
    private boolean isStock(
            Instrument instrument) {

        return instrument != null
                && instrument.getAssetType() != null
                && "STOCK".equalsIgnoreCase(
                        instrument.getAssetType());
    }

    private boolean isCrypto(
            Instrument instrument) {

        return instrument != null
                && instrument.getAssetType() != null
                && "CRYPTO".equalsIgnoreCase(
                        instrument.getAssetType());
    }

    private boolean isForex(
            Instrument instrument) {

        return instrument != null
                && instrument.getAssetType() != null
                && "FOREX".equalsIgnoreCase(
                        instrument.getAssetType());
    }

    private boolean isCryptoTicker(
            String ticker) {

        return ticker != null
                && ticker.contains("-");
    }

    private boolean isForexTicker(
            String ticker) {

        return ticker != null
                && ticker.length() == 6
                && !ticker.contains("-");
    }

    // Main method to change the - to an / for uploading crypto tickers into the db
    private String toAlpacaCryptoSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        return ticker.replace("-", "/");
    }

    private String toAlpacaForexSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        if (ticker.contains("/")) {
            return ticker;
        }

        if (ticker.length() != 6) {

            throw new IllegalArgumentException(
                    "Invalid forex ticker: " + ticker);
        }

        return ticker.substring(0, 3)
                + "/"
                + ticker.substring(3);
    }


    // Checking the day and time to ensure the market is open
    private boolean isUsMarketHours() {

        OffsetDateTime easternNow =
                OffsetDateTime.now(
                        ZoneId.of(
                                "America/New_York"));

        DayOfWeek day =
                easternNow.getDayOfWeek();

        LocalTime time =
                easternNow.toLocalTime();

        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !time.isBefore(
                        LocalTime.of(9, 30))
                && time.isBefore(
                        LocalTime.of(16, 0));
    }


    // Returns information on the RefreshStatus
    public synchronized RefreshStatus getRefreshStatus() {

        return new RefreshStatus(
                lastRefreshStartedAt,
                lastRefreshCompletedAt,
                lastRefreshFailure,
                lastSuccessfulTickerCount,
                lastFailedTickerCount);
    }



    // ====================================
    // Repository Section Link
    // ====================================
    public List<Map<String, Object>> getLatestPrices() {

        return marketDataRepository.findLatestPrices();
    }

    public List<Map<String, Object>> getLatestPrice(
            String ticker) {

        return marketDataRepository.findLatestPrice(
                ticker.toUpperCase());
    }

    public List<Map<String, Object>> getPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {

        return marketDataRepository.findPriceHistory(
                ticker,
                from,
                to);
    }

    public List<String> getTickers() {

        return marketDataRepository.findTickers();
    }

    private void ensurePricesSchema() {

        marketDataRepository.ensurePricesSchema();
    }


    // =============================
    // Instrument and Alpaca Format
    // =============================
    public static class Instrument {

        private String ticker;
        private String assetType;

        public Instrument() {
        }

        public String getTicker() {
            return ticker;
        }

        public void setTicker(
                String ticker) {

            this.ticker = ticker;
        }

        public String getAssetType() {
            return assetType;
        }

        public void setAssetType(
                String assetType) {

            this.assetType = assetType;
        }

        @Override
        public String toString() {

            return "Instrument{" +
                    "ticker='" + ticker + '\'' +
                    ", assetType='" + assetType + '\'' +
                    '}';
        }
    }

    private record AlpacaQuotesResponse(
            @JsonProperty("quotes")
            Map<String, AlpacaQuote> quotes) {
    }

    private record AlpacaQuote(

            @JsonProperty("ap")
            BigDecimal askPrice,

            @JsonProperty("as")
            BigDecimal askSize,

            @JsonProperty("ax")
            String askExchange,

            @JsonProperty("bp")
            BigDecimal bidPrice,

            @JsonProperty("bs")
            BigDecimal bidSize,

            @JsonProperty("bx")
            String bidExchange,

            @JsonProperty("c")
            List<String> conditions,

            @JsonProperty("t")
            OffsetDateTime quoteTimestamp,

            @JsonProperty("z")
            String tape) {
    }

    public record RefreshStatus(
            OffsetDateTime lastStartedAt,
            OffsetDateTime lastCompletedAt,
            String lastFailure,
            int successfulTickerCount,
            int failedTickerCount) {
    }
}
