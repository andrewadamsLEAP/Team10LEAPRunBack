package com.example.repositories;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class MarketDataRepository {

    private final MarketDataMapper marketDataMapper;

    // Constructor for the repository facade. The mapper performs the database operations.
        public MarketDataRepository(MarketDataMapper marketDataMapper) {
                this.marketDataMapper = marketDataMapper;
    }

    // Find the last recorded_at timestamp for a given ticker. This is used to determine if we need to fetch new data from the Finnhub API.
    public OffsetDateTime findLastRecordedAt(String ticker) {
        return marketDataMapper.findLastRecordedAt(ticker);
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
        marketDataMapper.saveQuote(
                ticker, price, changeAmount, percentChange, previousClose, open, high, low, quoteTimestamp);
    }

    // Find the latest prices for a list of tickers. This is used to get the latest prices for the DOW 30 stocks.
    public List<Map<String, Object>> findLatestPrices(List<String> tickers) {
        return marketDataMapper.findLatestPrices(tickers);
    }

    // Find the most recent price for a given ticker. This is used to get the latest price for a single stock.
    public List<Map<String, Object>> findLatestPrice(String ticker) {
        return marketDataMapper.findLatestPrice(ticker);
    }
    
    // Find the price history for a given ticker between two dates. If the from and to dates are not provided, it defaults to the last 30 days.
    public List<Map<String, Object>> findPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {
        return marketDataMapper.findPriceHistory(ticker, from, to);
    }

    // Select all the tickers from the instruments table where the asset type is STOCK and order them by ticker.
    public List<String> findTickers() {
                return marketDataMapper.findTickers();
        }

        public void ensureDowInstruments(List<String> tickers) {
                marketDataMapper.ensureDowInstruments(tickers);
        }

        public void ensurePricesSchema() {
                marketDataMapper.ensurePricesSchema();
    }
}
