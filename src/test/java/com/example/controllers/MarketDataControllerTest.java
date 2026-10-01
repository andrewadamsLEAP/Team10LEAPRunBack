package com.example.controllers;

import com.example.services.MarketDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private MarketDataController marketDataController;

    /**
     * Tests getting latest prices endpoint
     */
    @Test
    void getLatestPrices_shouldReturnPricesFromService() {
        List<Map<String, Object>> mockPrices = new ArrayList<>();
        Map<String, Object> price = new HashMap<>();
        price.put("ticker", "AAPL");
        price.put("price", 150.25);
        mockPrices.add(price);

        when(marketDataService.getLatestPrices()).thenReturn(mockPrices);

        List<Map<String, Object>> result = marketDataController.getLatestPrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).get("ticker"));
        verify(marketDataService, times(1)).getLatestPrices();
    }

    /**
     * Tests refresh market data endpoint
     */
    @Test
    void refreshMarketData_shouldCallServiceAndReturnPrices() {
        List<Map<String, Object>> mockPrices = List.of(
                Map.of("ticker", "AAPL", "price", 150.0)
        );

        when(marketDataService.getLatestPrices()).thenReturn(mockPrices);

        List<Map<String, Object>> result = marketDataController.refreshMarketData();

        assertNotNull(result);
        verify(marketDataService, times(1)).refreshMarketData();
        verify(marketDataService, times(1)).getLatestPrices();
    }

    /**
     * Tests getting tickers endpoint
     */
    @Test
    void getTickers_shouldReturnTickersList() {
        List<String> mockTickers = List.of("AAPL", "GOOGL", "MSFT");

        when(marketDataService.getTickers()).thenReturn(mockTickers);

        List<String> result = marketDataController.getTickers();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("AAPL"));
        verify(marketDataService, times(1)).getTickers();
    }

    /**
     * Tests getting refresh status endpoint
     */
    @Test
    void getRefreshStatus_shouldReturnRefreshStatusObject() {
        MarketDataService.RefreshStatus mockStatus = mock(MarketDataService.RefreshStatus.class);

        when(marketDataService.getRefreshStatus()).thenReturn(mockStatus);

        MarketDataService.RefreshStatus result = marketDataController.getRefreshStatus();

        assertNotNull(result);
        verify(marketDataService, times(1)).getRefreshStatus();
    }

    /**
     * Tests getting latest price for specific ticker
     */
    @Test
    void getLatestPrice_shouldReturnPriceForTicker() {
        String ticker = "AAPL";
        List<Map<String, Object>> mockPrice = List.of(
                Map.of("ticker", ticker, "price", 150.25)
        );

        when(marketDataService.getLatestPrice(ticker)).thenReturn(mockPrice);

        List<Map<String, Object>> result = marketDataController.getLatestPrice(ticker);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(marketDataService, times(1)).getLatestPrice(ticker);
    }

    /**
     * Tests getting prices returns empty list when no data
     */
    @Test
    void getLatestPrices_shouldReturnEmptyListWhenNoData() {
        when(marketDataService.getLatestPrices()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = marketDataController.getLatestPrices();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting tickers returns empty list when no tickers
     */
    @Test
    void getTickers_shouldReturnEmptyListWhenNoTickers() {
        when(marketDataService.getTickers()).thenReturn(new ArrayList<>());

        List<String> result = marketDataController.getTickers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests refresh market data with multiple prices
     */
    @Test
    void refreshMarketData_shouldReturnMultiplePrices() {
        List<Map<String, Object>> mockPrices = List.of(
                Map.of("ticker", "AAPL", "price", 150.0),
                Map.of("ticker", "GOOGL", "price", 100.0),
                Map.of("ticker", "MSFT", "price", 200.0)
        );

        when(marketDataService.getLatestPrices()).thenReturn(mockPrices);

        List<Map<String, Object>> result = marketDataController.refreshMarketData();

        assertEquals(3, result.size());
        verify(marketDataService, times(1)).refreshMarketData();
    }
}
