package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.DTOs.instruments.InstrumentDetail;
import com.example.DTOs.instruments.PriceHistoryPoint;
import com.example.repositories.MarketDataRepository;
import com.example.generalServices.AlpacaClient;
import com.example.generalServices.FrankfurterClient;
import com.example.mappers.MarketSymbolMapper;
import com.example.services.InstrumentService;
import com.example.generalServices.AlpacaClient.AlpacaQuotesResponse;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;
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
        private final InstrumentService instrumentService;
        private final FrankfurterClient frankfurterClient;
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
        private LocalDate lastStockStatsRefreshDate;

    public MarketDataService(
            MarketDataRepository marketDataRepository,
            AlpacaClient alpacaClient,
            InstrumentService instrumentService,
            FrankfurterClient frankfurterClient,
            MarketSymbolMapper marketSymbolMapper,
            MarketHoursService marketHoursService,
            Environment environment,

            @Value("${alpaca.refresh-all}")
            boolean refreshAll,

            @Value("${alpaca.refresh-forex}")
            boolean refreshForex,

            @Value("${alpaca.skip-outside-market-hours}")
            boolean skipOutsideMarketHours) {

        this.marketDataRepository =
                marketDataRepository;

        this.alpacaClient =
                alpacaClient;

        this.instrumentService =
                instrumentService;

        this.frankfurterClient =
                frankfurterClient;

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

                                LocalDate today = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate();
                                if (!today.equals(lastStockStatsRefreshDate)
                                                && refreshStockInstrumentStats(stockSymbols)) {
                                        lastStockStatsRefreshDate = today;
                                }
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

            if (response != null && response.quotes() != null) {
                logger.info("Stock quotes received:");
                response.quotes().forEach((ticker, quote) -> {
                    logger.info(
                            "  {} - Ask: {} (size: {}), Bid: {} (size: {}) @ {}",
                            ticker,
                            quote.askPrice(),
                            quote.askSize(),
                            quote.bidPrice(),
                            quote.bidSize(),
                            quote.quoteTimestamp());
                    try {
                        marketDataRepository.saveQuote(
                                ticker,
                                quote.askPrice(),
                                quote.askSize(),
                                quote.askExchange(),
                                quote.bidPrice(),
                                quote.bidSize(),
                                quote.bidExchange(),
                                quote.tape(),
                                quote.quoteTimestamp());
                        lastSuccessfulTickerCount++;
                    } catch (Exception ex) {
                        lastFailedTickerCount++;
                        logger.error("Failed to save quote for ticker: {}", ticker, ex);
                    }
                });
            }

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
        // Stock Instrument Statistics
        //
        // Daily bars populate the four mutable statistics stored on
        // instruments. Ticker and asset type remain unchanged.
        // =========================================================

        private boolean refreshStockInstrumentStats(List<String> symbols) {
                try {
                        AlpacaClient.AlpacaBarsResponse response =
                                        alpacaClient.getDailyStockBars(symbols);

                        if (response == null || response.bars() == null) {
                                return false;
                        }

                        response.bars().forEach((ticker, bars) -> {
                                if (bars == null || bars.isEmpty()) {
                                        logger.warn("No daily bars returned for stock {}", ticker);
                                        return;
                                }

                                AlpacaClient.AlpacaBar latest = bars.get(bars.size() - 1);
                                AlpacaClient.AlpacaBar previous = bars.size() > 1
                                                ? bars.get(bars.size() - 2)
                                                : latest;

                                // Average the returned daily volumes instead of deriving volume from quotes.
                                double averageVolume = bars.stream()
                                                .map(AlpacaClient.AlpacaBar::volume)
                                                .filter(java.util.Objects::nonNull)
                                                .mapToDouble(java.math.BigDecimal::doubleValue)
                                                .average()
                                                .orElse(0.0);

                                int volume = latest.volume() == null
                                                ? 0
                                                : latest.volume().intValue();

                                // The previous bar is the previous trading session, not necessarily yesterday.
                                int updatedRows = instrumentService.updateMarketStats(
                                                ticker,
                                                previous.close() == null ? 0.0 : previous.close().doubleValue(),
                                                latest.open() == null ? 0.0 : latest.open().doubleValue(),
                                                volume,
                                                averageVolume);

                                if (updatedRows != 1) {
                                        logger.warn("Daily bars returned for unknown stock ticker {}", ticker);
                                }
                        });
                        return !response.bars().isEmpty();
                } catch (Exception exception) {
                        logger.error("Unable to refresh stock instrument statistics", exception);
                        return false;
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

            if (response != null && response.quotes() != null) {
                logger.info("Crypto quotes received:");
                response.quotes().forEach((ticker, quote) -> {
                    logger.info(
                            "  {} - Ask: {} (size: {}), Bid: {} (size: {}) @ {}",
                            ticker,
                            quote.askPrice(),
                            quote.askSize(),
                            quote.bidPrice(),
                            quote.bidSize(),
                            quote.quoteTimestamp());
                    try {
                        String databaseTicker = marketSymbolMapper.toDatabaseCryptoSymbol(ticker);
                        marketDataRepository.saveQuote(
                                databaseTicker,
                                quote.askPrice(),
                                quote.askSize(),
                                quote.askExchange(),
                                quote.bidPrice(),
                                quote.bidSize(),
                                quote.bidExchange(),
                                quote.tape(),
                                quote.quoteTimestamp());
                        lastSuccessfulTickerCount++;
                    } catch (Exception ex) {
                        lastFailedTickerCount++;
                        logger.error("Failed to save quote for ticker: {}", ticker, ex);
                    }
                });
            }

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

            Map<String, List<String>> quotesByBase = symbols.stream()
                    .map(symbol -> new String[] {
                            marketSymbolMapper.toFrankfurterBase(symbol),
                            marketSymbolMapper.toFrankfurterQuote(symbol)
                    })
                    .collect(java.util.stream.Collectors.groupingBy(
                            pair -> pair[0],
                            java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.mapping(
                                    pair -> pair[1],
                                    java.util.stream.Collectors.toList())));

            quotesByBase.forEach((base, quotes) -> {
                logger.info("Requesting Frankfurter rates for base {} and quotes {}", base, quotes);
                List<FrankfurterClient.FrankfurterRate> response =
                        frankfurterClient.getRates(base, quotes);
                response.forEach(rate -> {
                    logger.info(
                            "  {}/{} - Rate: {} @ {}",
                            rate.base(), rate.quote(), rate.rate(), rate.date());
                    try {
                        String normalizedPair = rate.base() + rate.quote();
                        String databaseTicker = symbols.stream()
                                .filter(symbol -> symbol.replace("-", "")
                                        .replace("/", "")
                                        .equalsIgnoreCase(normalizedPair))
                                .findFirst()
                                .orElse(normalizedPair);
                        OffsetDateTime quoteTimestamp = rate.date()
                                .atStartOfDay()
                                .atOffset(ZoneOffset.UTC);
                        marketDataRepository.saveQuote(
                                databaseTicker,
                                rate.rate(),
                                BigDecimal.ZERO,
                                "FOREX",
                                rate.rate(),
                                BigDecimal.ZERO,
                                "FOREX",
                                null,
                                quoteTimestamp);
                        lastSuccessfulTickerCount++;
                    } catch (Exception ex) {
                        lastFailedTickerCount++;
                        logger.error("Failed to save forex rate for {}/{}", rate.base(), rate.quote(), ex);
                    }
                });
            });

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

    public List<PriceHistoryPoint> getPriceHistoryPoints(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {

        return marketDataRepository
                        .findPriceHistoryPoints(
                                ticker.toUpperCase(),
                                from,
                                to);
    }

    public List<String> getTickers() {

        return marketDataRepository
                .findTickers();
    }

    public InstrumentDetail getInstrumentDetails(
            String ticker) {

        return marketDataRepository
                .findInstrumentDetails(
                        ticker.toUpperCase());
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
