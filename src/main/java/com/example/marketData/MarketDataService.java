package com.example.marketData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.generalServices.AlpacaClient;
import com.example.generalServices.MarketHoursService;
import com.example.generalServices.AlpacaClient.AlpacaForexResponse;
import com.example.generalServices.AlpacaClient.AlpacaQuotesResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    MarketDataService.class);

    private final MarketDataRepository marketDataRepository;
    private final AlpacaClient alpacaClient;
    private final MarketDataProcessor marketDataProcessor;
    private final MarketSymbolMapper marketSymbolMapper;
    private final MarketHoursService marketHoursService;
    private final Environment environment;

    private final boolean refreshAll;
    private final boolean refreshForex;
    private final boolean skipOutsideMarketHours;

    private OffsetDateTime lastRefreshStartedAt;
    private OffsetDateTime lastRefreshCompletedAt;
    private String lastRefreshFailure;

    private int lastSuccessfulTickerCount;
    private int lastFailedTickerCount;

    public MarketDataService(
            MarketDataRepository marketDataRepository,
            AlpacaClient alpacaClient,
            MarketDataProcessor marketDataProcessor,
            MarketSymbolMapper marketSymbolMapper,
            MarketHoursService marketHoursService,
            Environment environment,

            @Value("${alpaca.refresh-all:true}")
            boolean refreshAll,

            @Value("${alpaca.refresh-forex:false}")
            boolean refreshForex,

            @Value("${alpaca.skip-outside-market-hours:false}")
            boolean skipOutsideMarketHours) {

        this.marketDataRepository =
                marketDataRepository;

        this.alpacaClient =
                alpacaClient;

        this.marketDataProcessor =
                marketDataProcessor;

        this.marketSymbolMapper =
                marketSymbolMapper;

        this.marketHoursService =
                marketHoursService;

        this.environment =
                environment;

        this.refreshAll =
                refreshAll;

        this.refreshForex =
                refreshForex;

        this.skipOutsideMarketHours =
                skipOutsideMarketHours;
    }

    // =========================================================
    // Scheduled Refresh
    // =========================================================

    @Scheduled(
            fixedDelayString =
                    "${alpaca.refresh-delay-ms:2000}")
    public void scheduledRefreshMarketData() {

        logger.info(
                "Starting scheduled market-data refresh. " +
                "Alpaca credentials configured: key={}, secret={}",
                alpacaClient.isApiKeyConfigured(),
                alpacaClient.isApiSecretConfigured());

        /*
         * Do not make real Alpaca requests while running tests.
         */
        if (environment.acceptsProfiles(
                Profiles.of("test"))) {

            return;
        }

        if (!alpacaClient.isApiKeyConfigured()) {

            logger.warn(
                    "ALPACA_API_KEY is not configured");

            return;
        }

        if (!alpacaClient.isApiSecretConfigured()) {

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

    // =========================================================
    // Database Initialization
    // =========================================================

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMarketDataStorage() {

        /*
         * Do not initialize the prices schema during tests.
         */
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

    // =========================================================
    // Main Refresh Workflow
    // =========================================================

    public synchronized void refreshMarketData() {

        lastRefreshStartedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        lastRefreshFailure = null;

        lastSuccessfulTickerCount = 0;

        lastFailedTickerCount = 0;

        alpacaClient.validateCredentials();

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

        /*
         * If refresh-all=false, only refresh
         * the first configured instrument.
         */
        if (!refreshAll) {

            instruments =
                    List.of(
                            instruments.get(0));
        }

        List<Instrument> instrumentsToRefresh =
                new ArrayList<>(
                        instruments);

        // =====================================================
        // Separate Instruments By Asset Type
        // =====================================================

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

        // =====================================================
        // STOCKS
        // =====================================================

        if (!stockSymbols.isEmpty()) {

            if (skipOutsideMarketHours
                    && !marketHoursService
                            .isUsMarketHours()) {

                logger.info(
                        "Skipping stock refresh because " +
                        "U.S. market is closed");

            } else {

                refreshStocks(
                        stockSymbols);
            }
        }

        // =====================================================
        // CRYPTO
        // =====================================================

        if (!cryptoSymbols.isEmpty()) {

            refreshCrypto(
                    cryptoSymbols);
        }

        // =====================================================
        // FOREX
        // =====================================================

        if (refreshForex
                && !forexSymbols.isEmpty()) {

            refreshForex(
                    forexSymbols);

        } else if (!refreshForex
                && !forexSymbols.isEmpty()) {

            logger.info(
                    "Forex refresh is disabled by configuration");
        }

        // =====================================================
        // Complete
        // =====================================================

        lastRefreshCompletedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC);

        logger.info(
                "Market-data refresh complete. " +
                "Successful: {}, Failed: {}",
                lastSuccessfulTickerCount,
                lastFailedTickerCount);
    }

    // =========================================================
    // Stock Refresh
    // =========================================================

    private void refreshStocks(
            List<String> symbols) {

        try {

            logger.info(
                    "Requesting stock quotes for {} symbols: {}",
                    symbols.size(),
                    symbols);

            AlpacaClient.AlpacaQuotesResponse response =
                    alpacaClient.getStockQuotes(
                            symbols);

            MarketDataProcessor.ProcessResult result =
                    marketDataProcessor.processQuotes(
                            symbols,
                            response);

            lastSuccessfulTickerCount +=
                    result.successfulCount();

            lastFailedTickerCount +=
                    result.failedCount();

        } catch (Exception exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh stock quotes",
                    exception);
        }
    }

    // =========================================================
    // Crypto Refresh
    // =========================================================

    private void refreshCrypto(
            List<String> symbols) {

        try {

            /*
             * Convert database symbols such as:
             *
             * BTC-USD
             *
             * into Alpaca symbols such as:
             *
             * BTC/USD
             */
            String symbolParameter =
                    marketSymbolMapper
                            .toAlpacaCryptoSymbols(
                                    symbols);

            logger.info(
                    "Requesting crypto quotes for {} symbols: {}",
                    symbols.size(),
                    symbolParameter);

            AlpacaClient.AlpacaQuotesResponse response =
                    alpacaClient.getCryptoQuotes(
                            symbolParameter);

            MarketDataProcessor.ProcessResult result =
                    marketDataProcessor.processQuotes(
                            symbols,
                            response);

            lastSuccessfulTickerCount +=
                    result.successfulCount();

            lastFailedTickerCount +=
                    result.failedCount();

        } catch (Exception exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh crypto quotes",
                    exception);
        }
    }

    // =========================================================
    // Forex Refresh
    // =========================================================

    private void refreshForex(
            List<String> symbols) {

        try {

            /*
             * Forex is different from stocks and crypto.
             *
             * Alpaca expects:
             *
             * currency_pairs=EUR/USD,USD/JPY
             *
             * rather than:
             *
             * symbols=EUR/USD,USD/JPY
             */
            String currencyPairs =
                    marketSymbolMapper
                            .toAlpacaForexSymbols(
                                    symbols);

            logger.info(
                    "Requesting forex rates for {} symbols: {}",
                    symbols.size(),
                    currencyPairs);

            /*
             * Forex returns AlpacaForexResponse,
             * NOT AlpacaQuotesResponse.
             */
            AlpacaClient.AlpacaForexResponse response =
                    alpacaClient.getForexRates(
                            currencyPairs);

            /*
             * Forex also has its own processor because
             * AlpacaForexRate has a different structure
             * from AlpacaQuote.
             */
            MarketDataProcessor.ProcessResult result =
                    marketDataProcessor.processForexRates(
                            symbols,
                            response);

            lastSuccessfulTickerCount +=
                    result.successfulCount();

            lastFailedTickerCount +=
                    result.failedCount();

        } catch (Exception exception) {

            lastRefreshFailure =
                    exception.getMessage();

            lastFailedTickerCount +=
                    symbols.size();

            logger.error(
                    "Unable to refresh forex rates",
                    exception);
        }
    }

    // =========================================================
    // Instrument Type Checks
    // =========================================================

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

    // =========================================================
    // Refresh Status
    // =========================================================

    public synchronized RefreshStatus getRefreshStatus() {

        return new RefreshStatus(
                lastRefreshStartedAt,
                lastRefreshCompletedAt,
                lastRefreshFailure,
                lastSuccessfulTickerCount,
                lastFailedTickerCount);
    }

    // =========================================================
    // Repository / Read Operations
    // =========================================================

    public List<Map<String, Object>> getLatestPrices() {

        return marketDataRepository
                .findLatestPrices();
    }

    public List<Map<String, Object>> getLatestPrice(
            String ticker) {

        return marketDataRepository
                .findLatestPrice(
                        ticker.toUpperCase());
    }

    public List<Map<String, Object>> getPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {

        return marketDataRepository
                .findPriceHistory(
                        ticker,
                        from,
                        to);
    }

    public List<String> getTickers() {

        return marketDataRepository
                .findTickers();
    }

    // =========================================================
    // Database Schema
    // =========================================================

    private void ensurePricesSchema() {

        marketDataRepository
                .ensurePricesSchema();
    }

    // =========================================================
    // Instrument Model
    // =========================================================

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
                    "ticker='" +
                    ticker +
                    '\'' +
                    ", assetType='" +
                    assetType +
                    '\'' +
                    '}';
        }
    }

    // =========================================================
    // Refresh Status Model
    // =========================================================

    public record RefreshStatus(
            OffsetDateTime lastStartedAt,
            OffsetDateTime lastCompletedAt,
            String lastFailure,
            int successfulTickerCount,
            int failedTickerCount) {
    }
}

