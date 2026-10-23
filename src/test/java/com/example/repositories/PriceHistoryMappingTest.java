package com.example.repositories;

import com.example.mappers.MarketDataMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceHistoryMappingTest {
    @Test
    void mapsImmutableHistoryPointsInTimeOrderWithPriceFallback() throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE prices (
                            ticker VARCHAR(20),
                            bid_price NUMERIC(18,4),
                            ask_price NUMERIC(18,4),
                            recorded_at TIMESTAMP WITH TIME ZONE
                        )
                        """);
                statement.execute("""
                        INSERT INTO prices VALUES
                        ('AAPL', NULL, 101.5, '2026-10-22 11:00:00+00'),
                        ('AAPL', 100, 102, '2026-10-22 10:00:00+00'),
                        ('AAPL', 98, 99, '2026-10-22 10:00:10+00'),
                        ('AAPL', NULL, NULL, '2026-10-22 12:00:00+00'),
                        ('MSFT', 999, 1000, '2026-10-22 10:00:00+00')
                        """);
            }
            var configuration = new Configuration(new Environment(
                    "test", new JdbcTransactionFactory(), dataSource));
            configuration.addMapper(MarketDataMapper.class);
            try (var session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
                var mapper = session.getMapper(MarketDataMapper.class);
                var from = OffsetDateTime.parse("2026-10-22T09:00:00Z");
                var to = OffsetDateTime.parse("2026-10-22T12:00:00Z");
                var points = mapper.findPriceHistoryPoints("AAPL", from, to);

                assertEquals(2, points.size());
                assertEquals("AAPL", points.get(0).ticker());
                assertEquals(0, points.get(0).price().compareTo(new BigDecimal("98")));
                assertEquals(OffsetDateTime.parse("2026-10-22T10:00:10Z"), points.get(0).timestamp());
                assertEquals(0, points.get(1).price().compareTo(new BigDecimal("101.5")));
                assertTrue(mapper.findPriceHistoryPoints("AAPL", to.plusDays(1), to.plusDays(2)).isEmpty());
                assertEquals(2, mapper.findPriceHistoryPoints("AAPL", from.minusDays(6), to).size());
                var monthly = mapper.findPriceHistoryPoints("AAPL", from.minusDays(29), to);
                assertEquals(2, monthly.size());
                assertEquals(0, monthly.get(0).price().compareTo(new BigDecimal("99")));
                assertEquals(OffsetDateTime.parse("2026-10-22T10:00:10Z"), monthly.get(0).timestamp());
                assertEquals(0, monthly.get(1).price().compareTo(new BigDecimal("101.5")));
                var yearly = mapper.findPriceHistoryPoints("AAPL", from.minusDays(364), to);
                assertEquals(1, yearly.size());
                assertEquals(0, yearly.get(0).price().compareTo(new BigDecimal("101.5")));
            }
        }
    }
}
