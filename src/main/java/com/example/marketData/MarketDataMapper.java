package com.example.marketData;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.marketData.MarketDataService.Instrument;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

//===================================
// Here the is general format for a mapper
// A SQL Query
// The name of the method that query is pointing to with its data type
// The parameters and or results
//===================================

// TO DOS
// Add comments on each method to go further in detail about each SQL query


@Mapper
public interface MarketDataMapper {

    @Select("""
        SELECT recorded_at
        FROM prices
        WHERE ticker = #{ticker}
        ORDER BY recorded_at DESC
        LIMIT 1
        """)
    OffsetDateTime findLastRecordedAt(
            @Param("ticker") String ticker);

    @Select("""
        SELECT
            ticker,
            asset_type AS assetType
        FROM instruments
        ORDER BY asset_type, ticker
        """)
    @Results({
            @Result(
                    property = "ticker",
                    column = "ticker"),

            @Result(
                    property = "assetType",
                    column = "assetType")
    })
    List<Instrument> findInstruments();

    @Select("""
        SELECT ticker
        FROM instruments
        ORDER BY ticker
        """)
    List<String> findTickers();

    @Insert("""
        INSERT INTO prices (
            ticker,
            ask_price,
            ask_size,
            ask_exchange,
            bid_price,
            bid_size,
            bid_exchange,
            tape,
            quote_timestamp
        )
        VALUES (
            #{ticker},
            #{askPrice},
            #{askSize},
            #{askExchange},
            #{bidPrice},
            #{bidSize},
            #{bidExchange},
            #{tape},
            #{quoteTimestamp}
        )
        """)
    void saveQuote(

            @Param("ticker")
            String ticker,

            @Param("askPrice")
            BigDecimal askPrice,

            @Param("askSize")
            BigDecimal askSize,

            @Param("askExchange")
            String askExchange,

            @Param("bidPrice")
            BigDecimal bidPrice,

            @Param("bidSize")
            BigDecimal bidSize,

            @Param("bidExchange")
            String bidExchange,

            @Param("tape")
            String tape,

            @Param("quoteTimestamp")
            OffsetDateTime quoteTimestamp);

    @Select("""
        <script>
        SELECT
            ticker,
            ask_price,
            ask_size,
            ask_exchange,
            bid_price,
            bid_size,
            bid_exchange,
            tape,
            quote_timestamp,
            recorded_at
        FROM (
            SELECT
                p.*,
                ROW_NUMBER() OVER (
                    PARTITION BY p.ticker
                    ORDER BY p.recorded_at DESC
                ) AS rn
            FROM prices p
            WHERE p.ticker IN
            <foreach
                collection="tickers"
                item="ticker"
                open="("
                separator=","
                close=")">
                #{ticker}
            </foreach>
        ) latest
        WHERE rn = 1
        ORDER BY ticker
        </script>
        """)
    List<Map<String, Object>> findLatestPrices(
            @Param("tickers")
            List<String> tickers);

    @Select("""
        SELECT
            p.ticker,
            p.ask_price,
            p.ask_size,
            p.ask_exchange,
            p.bid_price,
            p.bid_size,
            p.bid_exchange,
            p.tape,
            p.quote_timestamp,
            p.recorded_at,
            i.asset_type
        FROM prices p
        JOIN instruments i
            ON i.ticker = p.ticker
        WHERE p.recorded_at = (
            SELECT MAX(p2.recorded_at)
            FROM prices p2
            WHERE p2.ticker = p.ticker
        )
        ORDER BY p.ticker
        """)
    List<Map<String, Object>> findAllLatestPrices();

    @Select("""
        SELECT
            p.ticker,
            p.ask_price,
            p.ask_size,
            p.ask_exchange,
            p.bid_price,
            p.bid_size,
            p.bid_exchange,
            p.tape,
            p.quote_timestamp,
            p.recorded_at,
            i.asset_type
        FROM prices p
        JOIN instruments i
            ON i.ticker = p.ticker
        WHERE p.ticker = #{ticker}
        ORDER BY p.recorded_at DESC
        LIMIT 1
        """)
    List<Map<String, Object>> findLatestPrice(
            @Param("ticker")
            String ticker);

    @Select("""
        SELECT
            ticker,
            ask_price,
            ask_size,
            ask_exchange,
            bid_price,
            bid_size,
            bid_exchange,
            tape,
            quote_timestamp,
            recorded_at
        FROM prices
        WHERE ticker = #{ticker}
          AND recorded_at >= #{from}
          AND recorded_at <= #{to}
        ORDER BY recorded_at
        """)
    List<Map<String, Object>> findPriceHistory(

            @Param("ticker")
            String ticker,

            @Param("from")
            OffsetDateTime from,

            @Param("to")
            OffsetDateTime to);

    @Update("""
        DO $$
        BEGIN

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS recorded_at
                TIMESTAMPTZ NOT NULL
                DEFAULT now();

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS ask_price
                NUMERIC(18,4);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS ask_size
                NUMERIC(18,4);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS ask_exchange
                VARCHAR(10);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS bid_price
                NUMERIC(18,4);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS bid_size
                NUMERIC(18,4);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS bid_exchange
                VARCHAR(10);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS tape
                VARCHAR(10);

            ALTER TABLE prices
                ADD COLUMN IF NOT EXISTS quote_timestamp
                TIMESTAMPTZ;

            CREATE INDEX IF NOT EXISTS
                idx_prices_ticker_recorded_at
            ON prices (
                ticker,
                recorded_at DESC
            );

        END $$;
        """)
    void ensurePricesSchema();
}
