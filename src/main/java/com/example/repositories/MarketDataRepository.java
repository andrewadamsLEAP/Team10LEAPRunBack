package com.example.repositories;

import com.example.services.MarketDataService.Instrument;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class MarketDataRepository {

        // Creating the new mapper object to talk to the MarketDataMapper

    private final MarketDataMapper marketDataMapper;

    public MarketDataRepository(
            MarketDataMapper marketDataMapper) {

        this.marketDataMapper =
                marketDataMapper;
    }

    // ================================
    // Lots of MarketDataMapper methods
    // ================================

    public List<Instrument> findInstruments() {

        return marketDataMapper.findInstruments();
    }

    public List<String> findTickers() {

        return marketDataMapper.findTickers();
    }

    public OffsetDateTime findLastRecordedAt(
            String ticker) {

        return marketDataMapper.findLastRecordedAt(
                ticker);
    }

    public void saveQuote(
            String ticker,
            BigDecimal askPrice,
            BigDecimal askSize,
            String askExchange,
            BigDecimal bidPrice,
            BigDecimal bidSize,
            String bidExchange,
            String tape,
            OffsetDateTime quoteTimestamp) {

        marketDataMapper.saveQuote(
                ticker,
                askPrice,
                askSize,
                askExchange,
                bidPrice,
                bidSize,
                bidExchange,
                tape,
                quoteTimestamp);
    }

    public List<Map<String, Object>> findLatestPrices(
            List<String> tickers) {

        return marketDataMapper.findLatestPrices(
                tickers);
    }

    public List<Map<String, Object>> findLatestPrices() {

        return marketDataMapper.findAllLatestPrices();
    }

    public List<Map<String, Object>> findLatestPrice(
            String ticker) {

        return marketDataMapper.findLatestPrice(
                ticker);
    }

    public List<Map<String, Object>> findPriceHistory(
            String ticker,
            OffsetDateTime from,
            OffsetDateTime to) {

        return marketDataMapper.findPriceHistory(
                ticker,
                from,
                to);
    }

    public void ensurePricesSchema() {

        marketDataMapper.ensurePricesSchema();
    }
}
