package com.example.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataRepositoryTest {

    @Test
    void findLatestPricesCreatesOnePlaceholderPerTicker() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        List<Map<String, Object>> expected = List.of(Map.of("ticker", "AAPL"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(expected);
        MarketDataRepository repository = new MarketDataRepository(jdbcTemplate);

        List<Map<String, Object>> actual = repository.findLatestPrices(List.of("AAPL", "MSFT"));

        assertSame(expected, actual);
        verify(jdbcTemplate).queryForList(
                org.mockito.ArgumentMatchers.contains("ticker IN (?,?)"),
                any(Object[].class));
    }

    @Test
    void findPriceHistoryUsesTickerAndDateBounds() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        MarketDataRepository repository = new MarketDataRepository(jdbcTemplate);
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-15T00:00:00Z");

        repository.findPriceHistory("AAPL", from, to);

        verify(jdbcTemplate).queryForList(
                org.mockito.ArgumentMatchers.contains("recorded_at >= ? AND recorded_at <= ?"),
                any(Object[].class));
    }
}
