package com.example.repositories;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MarketDataMapper {

    @Select("SELECT recorded_at FROM prices WHERE ticker = #{ticker} "
            + "ORDER BY recorded_at DESC LIMIT 1")
    OffsetDateTime findLastRecordedAt(@Param("ticker") String ticker);

    @Insert("INSERT INTO prices (ticker, price, change_amount, percent_change, previous_close, "
            + "open, high, low, quote_timestamp) "
            + "VALUES (#{ticker}, #{price}, #{changeAmount}, #{percentChange}, #{previousClose}, "
            + "#{open}, #{high}, #{low}, #{quoteTimestamp})")
    void saveQuote(
            @Param("ticker") String ticker,
            @Param("price") BigDecimal price,
            @Param("changeAmount") BigDecimal changeAmount,
            @Param("percentChange") BigDecimal percentChange,
            @Param("previousClose") BigDecimal previousClose,
            @Param("open") BigDecimal open,
            @Param("high") BigDecimal high,
            @Param("low") BigDecimal low,
            @Param("quoteTimestamp") OffsetDateTime quoteTimestamp);

    @Select({
            "<script>",
            "SELECT ticker, price, change_amount, percent_change, previous_close, open, high, low, "
                    + "quote_timestamp, recorded_at FROM (SELECT p.*, ROW_NUMBER() OVER "
                    + "(PARTITION BY ticker ORDER BY recorded_at DESC) AS row_number "
                    + "FROM prices p WHERE ticker IN ",
            "<foreach collection='tickers' item='ticker' open='(' separator=',' close=')'>",
            "#{ticker}",
            "</foreach>",
            ") latest WHERE row_number = 1 ORDER BY ticker",
            "</script>"
    })
    List<Map<String, Object>> findLatestPrices(@Param("tickers") List<String> tickers);

    @Select("SELECT ticker, price, change_amount, percent_change, previous_close, open, high, low, "
            + "quote_timestamp, recorded_at FROM prices WHERE ticker = #{ticker} "
            + "ORDER BY recorded_at DESC LIMIT 1")
    List<Map<String, Object>> findLatestPrice(@Param("ticker") String ticker);

    @Select("SELECT ticker, price, change_amount, percent_change, previous_close, open, high, low, "
            + "quote_timestamp, recorded_at FROM prices WHERE ticker = #{ticker} "
            + "AND recorded_at >= #{from} AND recorded_at <= #{to} ORDER BY recorded_at")
    List<Map<String, Object>> findPriceHistory(
            @Param("ticker") String ticker,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Select("SELECT ticker FROM instruments WHERE asset_type = 'STOCK' ORDER BY ticker")
    List<String> findTickers();

    @Insert({
            "<script>",
            "INSERT INTO instruments (ticker, asset_type) VALUES",
            "<foreach collection='tickers' item='ticker' separator=','>",
            "(#{ticker}, 'STOCK')",
            "</foreach>",
            "ON CONFLICT (ticker) DO NOTHING",
            "</script>"
    })
    void ensureDowInstruments(@Param("tickers") List<String> tickers);

    @Update("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'prices' AND column_name = 'timestamp'
                ) AND NOT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'prices' AND column_name = 'recorded_at'
                ) THEN
                    ALTER TABLE prices RENAME COLUMN "timestamp" TO recorded_at;
                END IF;

                ALTER TABLE prices ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ NOT NULL DEFAULT now();
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS change_amount NUMERIC(18,4);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS percent_change NUMERIC(18,4);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS previous_close NUMERIC(18,2);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS open NUMERIC(18,2);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS high NUMERIC(18,2);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS low NUMERIC(18,2);
                ALTER TABLE prices ADD COLUMN IF NOT EXISTS quote_timestamp TIMESTAMPTZ;
                CREATE INDEX IF NOT EXISTS idx_prices_ticker_recorded_at
                    ON prices (ticker, recorded_at DESC);
            END $$;
            """)
    void ensurePricesSchema();
}