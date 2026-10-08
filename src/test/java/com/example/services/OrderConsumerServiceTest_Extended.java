package com.example.services;

import com.example.entities.Order;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.OrdersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderConsumerService (Kafka message consumption)
 * Tests the order execution flow triggered by Kafka messages
 */
@DisplayName("Order Consumer Service Tests")
class OrderConsumerServiceTest {

    private OrderConsumerService orderConsumerService;
    private OrdersRepository ordersRepository;
    private HoldingsService holdingsService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ordersRepository = mock(OrdersRepository.class);
        holdingsService = mock(HoldingsService.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        orderConsumerService = new OrderConsumerService(ordersRepository, holdingsService);
    }

    @Nested
    @DisplayName("CONSUME ORDER from Kafka")
    class ConsumeOrder {

        @Test
        @DisplayName("Should successfully consume and execute valid order from Kafka")
        void consumeValidOrder() throws Exception {
            // Arrange
            Order order = createTestOrder(1L, 1L, "AAPL", Order.OrderType.BUY, 10, 
                    new BigDecimal("150.00"), Order.OrderStatus.PENDING);
            String orderJson = objectMapper.writeValueAsString(order);

            when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(1L)).thenReturn(
                    createTestOrder(1L, 1L, "AAPL", Order.OrderType.BUY, 10, 
                            new BigDecimal("150.00"), Order.OrderStatus.FULFILLED)
            );

            // Act
            orderConsumerService.consumeOrder(orderJson);

            // Assert
            verify(ordersRepository).updateOrderStatus(1L, Order.OrderStatus.FULFILLED);
            verify(ordersRepository).getOrderById(1L);
            verify(holdingsService).updateHoldingsForOrder(any(Order.class));
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void consumeMalformedJson() throws Exception {
            // Arrange
            String malformedJson = "{invalid json}";

            // Act & Assert
            assertDoesNotThrow(() -> orderConsumerService.consumeOrder(malformedJson));
            // Service should log error and continue
        }

        @Test
        @DisplayName("Should handle null order JSON")
        void consumeNullOrder() throws Exception {
            // Act & Assert
            assertDoesNotThrow(() -> orderConsumerService.consumeOrder(null));
        }

        @Test
        @DisplayName("Should deserialize Order correctly from JSON")
        void deserializeOrderFromJson() throws Exception {
            // Arrange
            Order order = createTestOrder(2L, 2L, "GOOGL", Order.OrderType.SELL, 5, 
                    new BigDecimal("2800.00"), Order.OrderStatus.PENDING);
            String orderJson = objectMapper.writeValueAsString(order);

            when(ordersRepository.updateOrderStatus(2L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(2L)).thenReturn(
                    createTestOrder(2L, 2L, "GOOGL", Order.OrderType.SELL, 5, 
                            new BigDecimal("2800.00"), Order.OrderStatus.FULFILLED)
            );

            // Act
            orderConsumerService.consumeOrder(orderJson);

            // Assert: Verify data integrity
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(holdingsService).updateHoldingsForOrder(captor.capture());
            Order executedOrder = captor.getValue();
            assertEquals(2L, executedOrder.getClientId());
            assertEquals("GOOGL", executedOrder.getTicker());
            assertEquals(5, executedOrder.getQuantity());
        }
    }

    @Nested
    @DisplayName("EXECUTE ORDER")
    class ExecuteOrder {

        @Test
        @DisplayName("Should execute PENDING order successfully")
        void executePendingOrder() {
            // Arrange
            Order order = createTestOrder(3L, 3L, "MSFT", Order.OrderType.BUY, 20, 
                    new BigDecimal("350.00"), Order.OrderStatus.PENDING);

            when(ordersRepository.updateOrderStatus(3L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(3L)).thenReturn(
                    createTestOrder(3L, 3L, "MSFT", Order.OrderType.BUY, 20, 
                            new BigDecimal("350.00"), Order.OrderStatus.FULFILLED)
            );

            // Act
            Order result = orderConsumerService.executeOrder(order);

            // Assert
            assertEquals(Order.OrderStatus.FULFILLED, result.getOrderStatus());
            verify(ordersRepository).updateOrderStatus(3L, Order.OrderStatus.FULFILLED);
            verify(holdingsService).updateHoldingsForOrder(any(Order.class));
        }

        @Test
        @DisplayName("Should reject non-PENDING orders")
        void rejectNonPendingOrder() {
            // Arrange
            Order order = createTestOrder(4L, 4L, "TSLA", Order.OrderType.BUY, 15, 
                    new BigDecimal("250.00"), Order.OrderStatus.FULFILLED);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order),
                    "Should reject orders that are not PENDING");
        }

        @Test
        @DisplayName("Should reject CANCELLED orders")
        void rejectCancelledOrder() {
            // Arrange
            Order order = createTestOrder(5L, 5L, "NFLX", Order.OrderType.SELL, 8, 
                    new BigDecimal("300.00"), Order.OrderStatus.CANCELLED);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order));
        }

        @Test
        @DisplayName("Should update holdings after successful execution")
        void updateHoldingsAfterExecution() {
            // Arrange
            Order order = createTestOrder(6L, 6L, "FB", Order.OrderType.BUY, 25, 
                    new BigDecimal("200.00"), Order.OrderStatus.PENDING);

            when(ordersRepository.updateOrderStatus(6L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(6L)).thenReturn(
                    createTestOrder(6L, 6L, "FB", Order.OrderType.BUY, 25, 
                            new BigDecimal("200.00"), Order.OrderStatus.FULFILLED)
            );

            // Act
            orderConsumerService.executeOrder(order);

            // Assert
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(holdingsService).updateHoldingsForOrder(captor.capture());
            Order capturedOrder = captor.getValue();
            assertEquals(6L, capturedOrder.getOrderId());
            assertEquals(Order.OrderStatus.FULFILLED, capturedOrder.getOrderStatus());
        }

        @Test
        @DisplayName("Should handle database update failure")
        void handleDatabaseUpdateFailure() {
            // Arrange
            Order order = createTestOrder(7L, 7L, "AMZN", Order.OrderType.SELL, 12, 
                    new BigDecimal("3500.00"), Order.OrderStatus.PENDING);

            // Database returns 0 rows updated (order no longer exists or already updated)
            when(ordersRepository.updateOrderStatus(7L, Order.OrderStatus.FULFILLED)).thenReturn(0);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order));
            verify(holdingsService, never()).updateHoldingsForOrder(any());
        }
    }

    @Nested
    @DisplayName("ERROR SCENARIOS")
    class ErrorScenarios {

        @Test
        @DisplayName("Should handle order not found by ID")
        void handleOrderNotFound() {
            // Arrange
            Order order = createTestOrder(999L, 1L, "TEST", Order.OrderType.BUY, 10, 
                    new BigDecimal("100.00"), Order.OrderStatus.PENDING);

            when(ordersRepository.updateOrderStatus(999L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(999L)).thenReturn(null);

            // Act & Assert
            assertThrows(InvalidArgumentsException.class, () -> orderConsumerService.executeOrder(order));
        }

        @Test
        @DisplayName("Should reject invalid order ID (null)")
        void rejectNullOrderId() {
            // Arrange
            Order order = createTestOrder(null, 1L, "TEST", Order.OrderType.BUY, 10, 
                    new BigDecimal("100.00"), Order.OrderStatus.PENDING);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order));
        }

        @Test
        @DisplayName("Should reject invalid order ID (zero)")
        void rejectZeroOrderId() {
            // Arrange
            Order order = createTestOrder(0L, 1L, "TEST", Order.OrderType.BUY, 10, 
                    new BigDecimal("100.00"), Order.OrderStatus.PENDING);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order));
        }

        @Test
        @DisplayName("Should reject invalid order ID (negative)")
        void rejectNegativeOrderId() {
            // Arrange
            Order order = createTestOrder(-1L, 1L, "TEST", Order.OrderType.BUY, 10, 
                    new BigDecimal("100.00"), Order.OrderStatus.PENDING);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> orderConsumerService.executeOrder(order));
        }

        @Test
        @DisplayName("Should handle holdings update failure gracefully")
        void handleHoldingsUpdateFailure() {
            // Arrange
            Order order = createTestOrder(8L, 8L, "TEST", Order.OrderType.BUY, 10, 
                    new BigDecimal("100.00"), Order.OrderStatus.PENDING);

            when(ordersRepository.updateOrderStatus(8L, Order.OrderStatus.FULFILLED)).thenReturn(1);
            when(ordersRepository.getOrderById(8L)).thenReturn(order);
            doThrow(new RuntimeException("Holdings update failed"))
                    .when(holdingsService).updateHoldingsForOrder(any());

            // Act & Assert
            assertThrows(RuntimeException.class, () -> orderConsumerService.executeOrder(order));
        }
    }

    // Helper method to create test orders
    private Order createTestOrder(Long orderId, Long clientId, String ticker, Order.OrderType type, 
                                   int quantity, BigDecimal price, Order.OrderStatus status) {
        return new Order(orderId, clientId, ticker, type, status, quantity, price, OffsetDateTime.now());
    }
}
