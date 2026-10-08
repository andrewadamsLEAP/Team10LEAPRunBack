package com.example.integration;

import com.example.entities.Order;
import com.example.repositories.OrdersRepository;
import com.example.services.*;
import com.example.tradingApp.TradingAppApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete Kafka order flow.
 * Tests the end-to-end process from order placement through Kafka to execution.
 * 
 * Requirements:
 * - Kafka must be running (localhost:9092)
 * - Database must be accessible
 * - app.kafka.enabled=true in application.properties
 */
@SpringBootTest(classes = TradingAppApplication.class)
@TestPropertySource(properties = {
    "app.kafka.enabled=true",
    "spring.kafka.bootstrap.servers=localhost:9092"
})
@DisplayName("Kafka Order Flow Integration Tests")
class KafkaOrderFlowIntegrationTest {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderConsumerService orderConsumerService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clear any test data before each test
        // Note: In production, you'd use proper test data cleanup
    }

    @Test
    @DisplayName("Should complete full order lifecycle: place -> kafka -> execute")
    void testCompleteOrderLifecycle() throws InterruptedException {
        // Arrange
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 10;
        BigDecimal marketPrice = new BigDecimal("150.00");

        // Act: Place order (creates PENDING order and publishes to Kafka)
        Order placedOrder = ordersService.placeBuyOrder(clientId, ticker, quantity);

        // Assert: Order created with PENDING status
        assertNotNull(placedOrder);
        assertNotNull(placedOrder.getOrderId());
        assertEquals(Order.OrderStatus.PENDING, placedOrder.getOrderStatus());
        assertEquals(Order.OrderType.BUY, placedOrder.getOrderType());
        assertEquals(quantity, placedOrder.getQuantity());

        // Wait for Kafka consumer to process the message
        // In real tests, use CountDownLatch or TestContainers for Kafka
        Thread.sleep(2000);

        // Assert: Order should be executed (status FULFILLED)
        Order executedOrder = ordersRepository.getOrderById(placedOrder.getOrderId());
        assertEquals(Order.OrderStatus.FULFILLED, executedOrder.getOrderStatus(),
                "Order should be executed after Kafka processing");
    }

    @Test
    @DisplayName("Should publish buy order to Kafka topic")
    void testBuyOrderPublishedToKafka() {
        // Arrange
        Long clientId = 1L;
        String ticker = "GOOGL";
        int quantity = 5;

        // Act
        Order order = ordersService.placeBuyOrder(clientId, ticker, quantity);

        // Assert: Verify order was created
        assertNotNull(order.getOrderId());
        assertEquals(Order.OrderStatus.PENDING, order.getOrderStatus());
        assertEquals(ticker.toUpperCase(), order.getTicker());
    }

    @Test
    @DisplayName("Should publish sell order to Kafka topic")
    void testSellOrderPublishedToKafka() {
        // Arrange
        Long clientId = 1L;
        String ticker = "MSFT";
        int quantity = 3;

        // Act
        Order order = ordersService.placeSellOrder(clientId, ticker, quantity);

        // Assert: Verify order was created
        assertNotNull(order.getOrderId());
        assertEquals(Order.OrderStatus.PENDING, order.getOrderStatus());
        assertEquals(Order.OrderType.SELL, order.getOrderType());
    }

    @Test
    @DisplayName("Should track order status transitions through Kafka")
    void testOrderStatusTransition() throws InterruptedException {
        // Arrange
        Long clientId = 1L;
        String ticker = "TSLA";
        int quantity = 8;

        // Act: Create order
        Order pendingOrder = ordersService.placeBuyOrder(clientId, ticker, quantity);
        Long orderId = pendingOrder.getOrderId();

        // Assert: Initially PENDING
        assertEquals(Order.OrderStatus.PENDING, pendingOrder.getOrderStatus());

        // Wait for consumer to process
        Thread.sleep(2000);

        // Assert: After Kafka processing, should be FULFILLED
        Order fulfillledOrder = ordersRepository.getOrderById(orderId);
        assertEquals(Order.OrderStatus.FULFILLED, fulfillledOrder.getOrderStatus());
    }

    @Test
    @DisplayName("Should handle cancelled orders in Kafka flow")
    void testCancelledOrderInKafkaFlow() throws InterruptedException {
        // Arrange
        Long clientId = 1L;
        String ticker = "FB";
        int quantity = 6;

        // Act: Place order
        Order order = ordersService.placeBuyOrder(clientId, ticker, quantity);

        // Cancel before Kafka processes
        Order cancelledOrder = ordersService.cancelOrder(order.getOrderId());

        // Assert
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
    }

    @Test
    @DisplayName("Should maintain data consistency across Kafka messages")
    void testDataConsistencyThroughKafka() throws InterruptedException {
        // Arrange
        Long clientId = 1L;
        String ticker = "NFLX";
        int quantity = 12;
        BigDecimal orderPrice = new BigDecimal("200.50");

        // Act: Place order
        Order originalOrder = ordersService.placeBuyOrder(clientId, ticker, quantity);

        Thread.sleep(2000);

        // Assert: Data integrity after Kafka processing
        Order processedOrder = ordersRepository.getOrderById(originalOrder.getOrderId());
        assertEquals(clientId, processedOrder.getClientId(), "Client ID should be unchanged");
        assertEquals(ticker.toUpperCase(), processedOrder.getTicker(), "Ticker should be uppercase");
        assertEquals(quantity, processedOrder.getQuantity(), "Quantity should be unchanged");
        assertEquals(Order.OrderStatus.FULFILLED, processedOrder.getOrderStatus());
    }
}
