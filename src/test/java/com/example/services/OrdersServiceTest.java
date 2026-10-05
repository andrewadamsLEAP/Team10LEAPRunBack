package com.example.services;

import com.example.DTOs.orders.OrderResponse;
import com.example.entities.Order;
import com.example.entities.Instrument;
import com.example.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private OrdersService ordersService;

    @BeforeEach
    void setUp() {
        ordersRepository = mock(OrdersRepository.class);
        marketHoursService = mock(MarketHoursService.class);
        holdingsService = mock(HoldingsService.class);
        orderDtoConverter = mock(OrderDtoConverter.class);
        clientsService = mock(ClientsService.class);
        instrumentService = mock(InstrumentService.class);
        
        ordersService = new OrdersService(
                ordersRepository,
                marketHoursService,
                holdingsService,
                orderDtoConverter,
                clientsService,
                instrumentService
        );
    }

    // ========== PLACE BUY ORDER TESTS ==========

    @Test
    void placeBuyOrderCreatesOrderWhenAllValidationsPass() {
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 10;
        BigDecimal price = new BigDecimal("150.00");
        BigDecimal cashAmount = new BigDecimal("5000.00");

        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(clientId, "user@test.com", "testuser", "Test", "User", cashAmount)
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(ordersRepository.getPendingBuyOrdersForClient(clientId)).thenReturn(java.util.Collections.emptyList());
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(1L, clientId, ticker, Order.OrderType.BUY, quantity, price)
        );

        Order result = ordersService.placeBuyOrder(clientId, ticker, quantity, price);

        assertNotNull(result);
        assertEquals(Order.OrderType.BUY, result.getOrderType());
        assertEquals(ticker.toUpperCase(), result.getTicker());
        verify(clientsService, times(2)).getClientProfile(clientId);
        verify(instrumentService).getInstrumentByTicker(ticker);
    }

    @Test
    void placeBuyOrderThrowsWhenMarketIsClosed() {
        when(instrumentService.getInstrumentByTicker("AAPL")).thenReturn(instrument("AAPL", "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(false);
        when(clientsService.getClientProfile(1L)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(1L, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );

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
        when(instrumentService.getInstrumentByTicker("AAPL")).thenReturn(instrument("AAPL", "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, "AAPL", 10, new BigDecimal("150.00"))
        );

        assertEquals("Client not found: 999", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "FAKE";
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, ticker, 10, new BigDecimal("150.00"))
        );

        assertEquals("Instrument not found: FAKE", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenQuantityIsNegative() {
        when(instrumentService.getInstrumentByTicker("AAPL")).thenReturn(instrument("AAPL", "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(1L)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(1L, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(1L, "AAPL", -5, new BigDecimal("150.00"))
        );

        assertEquals("Quantity must be greater than zero.", exception.getMessage());
    }

    @Test
    void placeBuyOrderThrowsWhenPriceIsNegative() {
        when(instrumentService.getInstrumentByTicker("AAPL")).thenReturn(instrument("AAPL", "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(1L)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(1L, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );

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
        when(clientsService.getClientProfile(clientId)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(clientId, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(ordersRepository.getPendingSellOrdersForClientAndTicker(clientId, ticker.toUpperCase()))
                .thenReturn(java.util.Collections.emptyList());
        when(holdingsService.getHolding(clientId, ticker.toUpperCase())).thenReturn(
                new com.example.DTOs.holdings.HoldingResponse(clientId, ticker, 10)
        );
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(2L, clientId, ticker, Order.OrderType.SELL, quantity, price)
        );

        Order result = ordersService.placeSellOrder(clientId, ticker, quantity, price);

        assertNotNull(result);
        assertEquals(Order.OrderType.SELL, result.getOrderType());
        verify(clientsService).getClientProfile(clientId);
        verify(instrumentService).getInstrumentByTicker(ticker);
    }

    @Test
    void placeSellOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "INVALID";
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(null);

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

    // ========== EXECUTE ORDER TESTS ==========

    @Test
    void executeOrderUpdatesPendingOrderAndHoldings() {
        Order pendingOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        pendingOrder.setOrderStatus(Order.OrderStatus.PENDING);

        when(ordersRepository.getOrderById(1L)).thenReturn(pendingOrder);
        when(ordersRepository.updateOrderStatus(1L, Order.OrderStatus.FULFILLED)).thenReturn(1);

        Order result = ordersService.executeOrder(1L);

        assertNotNull(result);
        verify(ordersRepository).updateOrderStatus(1L, Order.OrderStatus.FULFILLED);
        verify(holdingsService).updateHoldingsForOrder(any(Order.class));
    }

    @Test
    void executeOrderThrowsWhenOrderIsNotPending() {
        Order cancelledOrder = order(1L, 1L, "AAPL", Order.OrderType.BUY, 10, new BigDecimal("150.00"));
        cancelledOrder.setOrderStatus(Order.OrderStatus.CANCELLED);

        when(ordersRepository.getOrderById(1L)).thenReturn(cancelledOrder);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ordersService.executeOrder(1L)
        );

        assertTrue(exception.getMessage().contains("Only pending orders can be executed"));
    }

    // ========== HELPER METHODS ==========

    /**
     * Creates a mock Instrument with the given ticker and asset type.
     */
    private Instrument instrument(String ticker, String assetType) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setAssetType(assetType);
        return instrument;
    }

    /**
     * Creates a test Order with the given parameters.
     */
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
}
