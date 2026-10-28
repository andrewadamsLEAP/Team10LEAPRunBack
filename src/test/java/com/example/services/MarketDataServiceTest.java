package com.example.services;

import com.example.repositories.MarketDataRepository;
import com.example.generalServices.AlpacaClient;
import com.example.generalServices.FrankfurterClient;
import com.example.services.InstrumentService;
import com.example.mappers.MarketSymbolMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private AlpacaClient alpacaClient;

    @Mock
    private FrankfurterClient frankfurterClient;

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private MarketSymbolMapper marketSymbolMapper;

    @Mock
    private MarketHoursService marketHoursService;

    @Mock
    private Environment environment;

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        // Manually create the service since it has a complex constructor
        marketDataService = new MarketDataService(
                marketDataRepository,
                alpacaClient,
                instrumentService,
                frankfurterClient,
                marketSymbolMapper,
                marketHoursService,
                environment,
                true,  // refreshAll
                true,  // refreshForex
                false  // skipOutsideMarketHours
        );
    }

    /**
     * Tests getting latest prices from repository
     */
    @Test
    void getLatestPrices_shouldReturnListFromRepository() {
        List<Map<String, Object>> mockPrices = new ArrayList<>();
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("ticker", "AAPL");
        priceData.put("price", 150.25);
        mockPrices.add(priceData);

        when(marketDataRepository.findLatestPrices()).thenReturn(mockPrices);

        List<Map<String, Object>> result = marketDataService.getLatestPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).get("ticker"));
        verify(marketDataRepository, times(1)).findLatestPrices();
    }

    /**
     * Tests getting latest prices returns empty list when no data
     */
    @Test
    void getLatestPrices_shouldReturnEmptyListWhenNoData() {
        when(marketDataRepository.findLatestPrices()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = marketDataService.getLatestPrices();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting latest price for specific ticker
     */
    @Test
    void getLatestPrice_shouldReturnPriceForTicker() {
        String ticker = "AAPL";
        List<Map<String, Object>> mockPrice = new ArrayList<>();
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("ticker", ticker);
        priceData.put("price", 150.25);
        mockPrice.add(priceData);

        when(marketDataRepository.findLatestPrice(ticker)).thenReturn(mockPrice);

        List<Map<String, Object>> result = marketDataService.getLatestPrice(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ticker, result.get(0).get("ticker"));
    }

    /**
     * Tests getting all tickers
     */
    @Test
    void getTickers_shouldReturnListOfTickers() {
        List<String> mockTickers = List.of("AAPL", "GOOGL", "MSFT");

        when(marketDataRepository.findTickers()).thenReturn(mockTickers);

        List<String> result = marketDataService.getTickers();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("AAPL"));
        assertTrue(result.contains("GOOGL"));
        verify(marketDataRepository, times(1)).findTickers();
    }

    /**
     * Tests getting refresh status
     */
    @Test
    void getRefreshStatus_shouldReturnRefreshStatusInfo() {
        MarketDataService.RefreshStatus status = marketDataService.getRefreshStatus();

        assertNotNull(status);
        // Status object should have some properties
        assertDoesNotThrow(() -> status.toString());
    }

    /**
     * Tests that refresh market data can be called
     */
    @Test
    void refreshMarketData_shouldNotThrowException() {
        assertDoesNotThrow(() -> marketDataService.refreshMarketData());
    }

    /**
     * Verifies that a tracked forex instrument is still persisted through
     * the existing quote storage contract.
     */
    @Test
    void refreshMarketData_shouldPersistTrackedFrankfurterForexRate() {
        MarketDataService.Instrument instrument = new MarketDataService.Instrument();
        instrument.setTicker("EUR-USD");
        instrument.setAssetType("FOREX");

        java.time.LocalDate latestDate = java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1);
        java.time.LocalDate previousDate = latestDate.minusDays(1);
        java.time.LocalDate to = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate from = to.minusDays(10);

        when(marketDataRepository.findInstruments()).thenReturn(List.of(instrument));
        when(marketSymbolMapper.toFrankfurterBase("EUR-USD")).thenReturn("EUR");
        when(marketSymbolMapper.toFrankfurterQuote("EUR-USD")).thenReturn("USD");
        when(instrumentService.updatePreviousClose("EUR-USD", 1.12)).thenReturn(1);
        when(frankfurterClient.getRates("EUR", List.of("USD"), from, to))
            .thenReturn(List.of(
                new FrankfurterClient.FrankfurterRate(
                previousDate,
                        "EUR",
                        "USD",
                new java.math.BigDecimal("1.1200")),
                new FrankfurterClient.FrankfurterRate(
                    latestDate,
                    "EUR",
                    "USD",
                    new java.math.BigDecimal("1.1238"))));

        marketDataService.refreshMarketData();
        marketDataService.refreshMarketData();

        verify(frankfurterClient, times(1)).getRates("EUR", List.of("USD"), from, to);
        verify(marketDataRepository, times(1)).saveQuote(
                eq("EUR-USD"),
                eq(new java.math.BigDecimal("1.1238")),
                eq(java.math.BigDecimal.ZERO),
                eq("FOREX"),
                eq(new java.math.BigDecimal("1.1238")),
                eq(java.math.BigDecimal.ZERO),
                eq("FOREX"),
                isNull(),
                eq(latestDate
                        .atStartOfDay()
                        .atOffset(java.time.ZoneOffset.UTC)));
            verify(instrumentService, times(1)).updatePreviousClose("EUR-USD", 1.12);
    }

            @Test
            void refreshMarketData_usesLastCompletedCryptoDailyBarAsPreviousClose() {
            MarketDataService.Instrument instrument = new MarketDataService.Instrument();
            instrument.setTicker("BTC-USD");
            instrument.setAssetType("CRYPTO");

            java.time.LocalDate today = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toLocalDate();
            java.time.LocalDate yesterday = today.minusDays(1);
            java.time.OffsetDateTime previousBarTime = yesterday.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            java.time.OffsetDateTime todayBarTime = today.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

            when(marketDataRepository.findInstruments()).thenReturn(List.of(instrument));
            when(marketSymbolMapper.toAlpacaCryptoSymbols(List.of("BTC-USD"))).thenReturn("BTC/USD");
            when(marketSymbolMapper.toAlpacaCryptoSymbol("BTC-USD")).thenReturn("BTC/USD");
            when(alpacaClient.getDailyCryptoBars(List.of("BTC/USD"))).thenReturn(
                new AlpacaClient.AlpacaBarsResponse(
                    Map.of("BTC/USD", List.of(
                        new AlpacaClient.AlpacaBar(
                            new java.math.BigDecimal("99"),
                            new java.math.BigDecimal("101"),
                            java.math.BigDecimal.ZERO,
                            previousBarTime),
                        new AlpacaClient.AlpacaBar(
                            new java.math.BigDecimal("101"),
                            new java.math.BigDecimal("150"),
                            java.math.BigDecimal.ZERO,
                            todayBarTime))),
                    null));
            when(instrumentService.updatePreviousClose("BTC-USD", 101.0)).thenReturn(1);

            marketDataService.refreshMarketData();
            marketDataService.refreshMarketData();

            verify(instrumentService, times(1)).updatePreviousClose("BTC-USD", 101.0);
            verify(alpacaClient, times(1)).getDailyCryptoBars(List.of("BTC/USD"));
            }

    /**
     * Tests that getting latest prices doesn't modify repository data
     */
    @Test
    void getLatestPrices_shouldNotModifyData() {
        List<Map<String, Object>> mockPrices = List.of(
                Map.of("ticker", "AAPL", "price", 150.0)
        );

        when(marketDataRepository.findLatestPrices()).thenReturn(new ArrayList<>(mockPrices));

        marketDataService.getLatestPrices();
        marketDataService.getLatestPrices();

        verify(marketDataRepository, times(2)).findLatestPrices();
    }

    /**
     * Tests getTickers with empty list
     */
    @Test
    void getTickers_shouldReturnEmptyListWhenNoTickers() {
        when(marketDataRepository.findTickers()).thenReturn(new ArrayList<>());

        List<String> result = marketDataService.getTickers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
