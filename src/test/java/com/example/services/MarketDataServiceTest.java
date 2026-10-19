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

        when(marketDataRepository.findInstruments()).thenReturn(List.of(instrument));
        when(marketSymbolMapper.toFrankfurterBase("EUR-USD")).thenReturn("EUR");
        when(marketSymbolMapper.toFrankfurterQuote("EUR-USD")).thenReturn("USD");
        when(frankfurterClient.getRates("EUR", List.of("USD")))
                .thenReturn(List.of(new FrankfurterClient.FrankfurterRate(
                        java.time.LocalDate.of(2026, 10, 19),
                        "EUR",
                        "USD",
                        new java.math.BigDecimal("1.1238"))));

        marketDataService.refreshMarketData();

        verify(frankfurterClient).getRates("EUR", List.of("USD"));
    verify(marketDataRepository).saveQuote(
                eq("EUR-USD"),
                eq(new java.math.BigDecimal("1.1238")),
                eq(java.math.BigDecimal.ZERO),
                eq("FRANKFURT"),
                eq(new java.math.BigDecimal("1.1238")),
                eq(java.math.BigDecimal.ZERO),
                eq("FRANKFURT"),
                isNull(),
                eq(java.time.LocalDate.of(2026, 10, 19)
                        .atStartOfDay()
                        .atOffset(java.time.ZoneOffset.UTC)));
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
