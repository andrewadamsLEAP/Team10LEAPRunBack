package com.example.repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class MarketDataRepository {

    private static final String PRICE_COLUMNS = "ticker, price, change_amount, percent_change, "
            + "previous_close, open, high, low, quote_timestamp, recorded_at";

    private final JdbcTemplate jdbcTemplate;

    // Constructor for the MarketDataRepository class. It takes a JdbcTemplate as a parameter, which is used to interact with the database.
    public MarketDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Find the last recorded_at timestamp for a given ticker. This is used to determine if we need to fetch new data from the Finnhub API.
    public OffsetDateTime findLastRecordedAt(String ticker) {
        List<OffsetDateTime> recordedAt = jdbcTemplate.query(
                "SELECT recorded_at FROM prices WHERE ticker = ? ORDER BY recorded_at DESC LIMIT 1",
                (resultSet, rowNum) -> resultSet.getObject("recorded_at", OffsetDateTime.class),
                ticker);
        return recordedAt.isEmpty() ? null : recordedAt.get(0);
    }

    // Main function to save a quote to the database. This is called by the MarketDataService after fetching a quote from the Finnhub API.
    public void saveQuote(
            String ticker,
            BigDecimal price,
            BigDecimal changeAmount,
            BigDecimal percentChange,
            BigDecimal previousClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            OffsetDateTime quoteTimestamp) {
        jdbcTemplate.update(
                "INSERT INTO prices (ticker, price, change_amount, percent_change, previous_close, "
                        + "open, high, low, quote_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ticker, price, changeAmount, percentChange, previousClose, open, high, low, quoteTimestamp);
    }

    // Find the latest prices for a list of tickers. This is used to get the latest prices for the DOW 30 stocks.
    public List<Map<String, Object>> findLatestPrices(List<String> tickers) {
        String placeholders = String.join(",", tickers.stream().map(ticker -> "?").toList());
        return jdbcTemplate.queryForList(
                "SELECT " + PRICE_COLUMNS
                        + " FROM (SELECT p.*, ROW_NUMBER() OVER "
                        + "(PARTITION BY ticker ORDER BY recorded_at DESC) AS row_number "
                        + "FROM prices p WHERE ticker IN (" + placeholders + ")) latest "
                        + "WHERE row_number = 1 ORDER BY ticker",
                tickers.toArray());
    }

    // Find the most recent price for a given ticker. This is used to get the latest price for a single stock.
    public List<Map<String, Object>> findLatestPrice(String ticker) {
        return jdbcTemplate.queryForList(
                "SELECT " + PRICE_COLUMNS
                        + " FROM prices WHERE ticker = ? ORDER BY recorded_at DESC LIMIT 1",
                ticker);
    }
    
    // Find the price history for a given ticker between two dates. If the from and to dates are not provided, it defaults to the last 30 days.
    public List<Map<String, Object>> findPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {
        return jdbcTemplate.queryForList(
                "SELECT " + PRICE_COLUMNS
                        + " FROM prices WHERE ticker = ? AND recorded_at >= ? AND recorded_at <= ?"
                        + " ORDER BY recorded_at",
                ticker, from, to);
    }

    // Select all the tickers from the instruments table where the asset type is STOCK and order them by ticker.
    public List<String> findTickers() {
        return jdbcTemplate.queryForList(
                "SELECT ticker FROM instruments WHERE asset_type = 'STOCK' ORDER BY ticker",
                String.class);
    }
}
