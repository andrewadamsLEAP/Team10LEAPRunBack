package com.example.services;

import com.example.DTOs.orders.OrderResponse;
import com.example.entities.Order;
import com.example.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdersServiceTest {

    private OrdersRepository ordersRepository;
    private MarketHoursService marketHoursService;
    private HoldingsService holdingsService;
    private OrderDtoConverter orderDtoConverter;
    private ClientsService clientsService;
    private InstrumentService instrumentService;
    private KafkaTemplate<String, Order> kafkaTemplate;
    private OrdersService ordersService;

    @BeforeEach
    void setUp() {
        ordersRepository = mock(OrdersRepository.class);
        marketHoursService = mock(MarketHoursService.class);
        holdingsService = mock(HoldingsService.class);
        orderDtoConverter = mock(OrderDtoConverter.class);
        clientsService = mock(ClientsService.class);
        instrumentService = mock(InstrumentService.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        
        ordersService = new OrdersService(
                ordersRepository,
                marketHoursService,
                holdingsService,
                orderDtoConverter,
                clientsService,
                instrumentService,
                kafkaTemplate,
                "order-pending-topic"
        );

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ========== PLACE BUY ORDER TESTS ==========

    @Test
    void placeBuyOrderCreatesOrderWhenAllValidationsPass() {
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 10;
        BigDecimal price = new BigDecimal("150.00");

        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(null); // Mock successful validation
        when(instrumentService.instrumentExists(ticker)).thenReturn(true);
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(1L, clientId, ticker, Order.OrderType.BUY, quantity, price)
        );

        Order result = ordersService.placeBuyOrder(clientId, ticker, quantity, price);
        triggerAfterCommitCallbacks();

        assertNotNull(result);
        assertEquals(Order.OrderType.BUY, result.getOrderType());
        assertEquals(ticker.toUpperCase(), result.getTicker());
        verify(clientsService).getClientProfile(clientId);
        verify(instrumentService).instrumentExists(ticker);
        verify(kafkaTemplate).send("order-pending-topic", ticker, result);
    }

    @Test
    void placeBuyOrderThrowsWhenMarketIsClosed() {
        when(marketHoursService.isUsMarketHours()).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ordersService.placeBuyOrder(1L, "AAPL", 10, new BigDecimal("150.00"))
        );

        assertEquals("Orders can only be placed during US market hours.", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenClientDoesNotExist() {
        Long clientId = 999L;
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, "AAPL", 10, new BigDecimal("150.00"))
        );

        assertEquals("Client not found: 999", exception.getMessage());
        verify(instrumentService, never()).instrumentExists(anyString());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "FAKE";
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(null);
        when(instrumentService.instrumentExists(ticker)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, ticker, 10, new BigDecimal("150.00"))
        );

        assertEquals("Instrument not found: FAKE", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenQuantityIsNegative() {
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(1L)).thenReturn(null);
        when(instrumentService.instrumentExists("AAPL")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(1L, "AAPL", -5, new BigDecimal("150.00"))
        );

        assertEquals("Quantity must be greater than zero.", exception.getMessage());
    }

    @Test
    void placeBuyOrderThrowsWhenPriceIsNegative() {
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(1L)).thenReturn(null);
        when(instrumentService.instrumentExists("AAPL")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(1L, "AAPL", 10, new BigDecimal("-50.00"))
        );

        assertEquals("Price must be greater than zero.", exception.getMessage());
    }

    @Test
    void placeBuyOrderThrowsWhenClientIdIsNull() {
        when(marketHoursService.isUsMarketHours()).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(null, "AAPL", 10, new BigDecimal("150.00"))
        );

        assertEquals("Client ID must be greater than zero.", exception.getMessage());
    }

    // ========== PLACE SELL ORDER TESTS ==========

    @Test
    void placeSellOrderCreatesOrderWhenAllValidationsPass() {
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 5;
        BigDecimal price = new BigDecimal("160.00");

        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(null);
        when(instrumentService.instrumentExists(ticker)).thenReturn(true);
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(2L, clientId, ticker, Order.OrderType.SELL, quantity, price)
        );

        Order result = ordersService.placeSellOrder(clientId, ticker, quantity, price);
        triggerAfterCommitCallbacks();

        assertNotNull(result);
        assertEquals(Order.OrderType.SELL, result.getOrderType());
        verify(clientsService).getClientProfile(clientId);
        verify(instrumentService).instrumentExists(ticker);
        verify(kafkaTemplate).send("order-pending-topic", ticker, result);
    }

    @Test
    void publishOrderAfterCommitThrowsWhenNoSynchronizationIsActive() {
        TransactionSynchronizationManager.clearSynchronization();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> ordersService.publishOrderAfterCommit(
                order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"))
            )
        );

        assertEquals(
            "No active transaction synchronization; order publication must occur within a transaction.",
            exception.getMessage()
        );
    }

    @Test
    void placeSellOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "INVALID";
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(null);
        when(instrumentService.instrumentExists(ticker)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeSellOrder(clientId, ticker, 5, new BigDecimal("160.00"))
        );

        assertEquals("Instrument not found: INVALID", exception.getMessage());
    }

    // ========== CANCEL ORDER TESTS ==========

    @Test
    void cancelOrderCancelsWhenOrderIsPending() {
        Order pendingOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        pendingOrder.setOrderStatus(Order.OrderStatus.PENDING);

        when(ordersRepository.getOrderById(1L)).thenReturn(pendingOrder);
        when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.CANCELLED)).thenReturn(1);

        Order result = ordersService.cancelOrder(1L);

        assertNotNull(result);
        verify(ordersRepository).updateOrderStatus(1L, Order.OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrderThrowsWhenOrderIsNotPending() {
        Order fulfilledOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        fulfilledOrder.setOrderStatus(Order.OrderStatus.FULFILLED);

        when(ordersRepository.getOrderById(1L)).thenReturn(fulfilledOrder);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ordersService.cancelOrder(1L)
        );

        assertTrue(exception.getMessage().contains("cannot be cancelled"));
        verify(ordersRepository, never()).updateOrderStatus(anyLong(), any());
    }

    // ========== HELPER METHODS ==========

    private Order order(
            Long orderId,
            Long clientId,
            String ticker,
            Order.OrderType orderType,
            int quantity,
            BigDecimal price) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setClientId(clientId);
        order.setTicker(ticker);
        order.setOrderType(orderType);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setOrderDate(OffsetDateTime.now());
        return order;
    }

    private void triggerAfterCommitCallbacks() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
    }
}
