package com.example.repositories;

import org.junit.jupiter.api.Test;

import com.example.marketData.MarketDataMapper;
import com.example.marketData.MarketDataRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataRepositoryTest {

    @Test
    void findLatestPricesCreatesOnePlaceholderPerTicker() {
        MarketDataMapper marketDataMapper = mock(MarketDataMapper.class);
        List<Map<String, Object>> expected = List.of(Map.of("ticker", "AAPL"));
        when(marketDataMapper.findLatestPrices(List.of("AAPL", "MSFT"))).thenReturn(expected);
        MarketDataRepository repository = new MarketDataRepository(marketDataMapper);

        List<Map<String, Object>> actual = repository.findLatestPrices(List.of("AAPL", "MSFT"));

        assertSame(expected, actual);
        verify(marketDataMapper).findLatestPrices(List.of("AAPL", "MSFT"));
    }

    @Test
    void findPriceHistoryUsesTickerAndDateBounds() {
        MarketDataMapper marketDataMapper = mock(MarketDataMapper.class);
        when(marketDataMapper.findPriceHistory(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        MarketDataRepository repository = new MarketDataRepository(marketDataMapper);
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-15T00:00:00Z");

        repository.findPriceHistory("AAPL", from, to);

        verify(marketDataMapper).findPriceHistory("AAPL", from, to);
    }
}
