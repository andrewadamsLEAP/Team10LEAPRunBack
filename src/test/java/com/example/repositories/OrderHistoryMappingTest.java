package com.example.repositories;

import com.example.mappers.OrdersMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderHistoryMappingTest {
    @Test
    void mapsTimezoneLessOrderDateForHistoryReconstruction() throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE orders (
                            order_id BIGINT,
                            client_id BIGINT,
                            ticker VARCHAR(20),
                            order_type VARCHAR(10),
                            order_status VARCHAR(20),
                            quantity INTEGER,
                            price NUMERIC(18,2),
                            order_date TIMESTAMP
                        )
                        """);
                statement.execute("""
                        INSERT INTO orders VALUES
                        (1, 7, 'AAPL', 'BUY', 'FULFILLED', 2, 100.00,
                         TIMESTAMP '2026-10-20 12:00:00')
                        """);
            }
            var configuration = new Configuration(new Environment(
                    "test", new JdbcTransactionFactory(), dataSource));
            configuration.addMapper(OrdersMapper.class);
            try (var session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
                var orders = session.getMapper(OrdersMapper.class).getFulfilledOrders(7L);

                assertEquals(1, orders.size());
                assertEquals(
                        OffsetDateTime.parse("2026-10-20T12:00:00Z"),
                        orders.get(0).getOrderDate());
            }
        }
    }
}
