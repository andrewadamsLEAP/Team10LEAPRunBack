package com.example.services;

import com.example.DTOs.orders.OrderResponse;
import com.example.entities.Order;
import com.example.entities.Instrument;
import com.example.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdersServiceTest {

    private OrdersRepository ordersRepository;
    private MarketHoursService marketHoursService;
    private HoldingsService holdingsService;
    private OrderDtoConverter orderDtoConverter;
    private ClientsService clientsService;
    private InstrumentService instrumentService;
    private KafkaTemplate<Object, Object> kafkaTemplate;
    private MarketDataService marketDataService;
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
        marketDataService = mock(MarketDataService.class);
        
        ordersService = new OrdersService(
                ordersRepository,
                marketHoursService,
                holdingsService,
                orderDtoConverter,
                clientsService,
                instrumentService,
                marketDataService,
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
        BigDecimal askPrice = new BigDecimal("150.00");
        BigDecimal cashAmount = new BigDecimal("5000.00");

        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, askPrice, new BigDecimal("149.00")))
        );
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(clientId, "user@test.com", "testuser", "Test", "User", cashAmount)
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(ordersRepository.getPendingBuyOrdersForClient(clientId)).thenReturn(Collections.emptyList());
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(1L, clientId, ticker, Order.OrderType.BUY, quantity, askPrice)
        );

        Order result = ordersService.placeBuyOrder(clientId, ticker, quantity);

        assertNotNull(result);
        assertEquals(Order.OrderType.BUY, result.getOrderType());
        assertEquals(ticker.toUpperCase(), result.getTicker());
        verify(marketDataService).getLatestPrice(ticker.toUpperCase());
        verify(clientsService, times(2)).getClientProfile(clientId);
        verify(instrumentService).getInstrumentByTicker(ticker);
    }

    @Test
    void placeBuyOrderThrowsWhenMarketIsClosed() {
        String ticker = "AAPL";
        BigDecimal askPrice = new BigDecimal("150.00");
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, askPrice, new BigDecimal("149.00")))
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(false);
        when(clientsService.getClientProfile(1L)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(1L, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ordersService.placeBuyOrder(1L, ticker, 10)
        );

        assertEquals("Orders can only be placed during US market hours.", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenClientDoesNotExist() {
        Long clientId = 999L;
        String ticker = "AAPL";
        BigDecimal askPrice = new BigDecimal("150.00");
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, askPrice, new BigDecimal("149.00")))
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenThrow(new IllegalArgumentException("Client not found"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, ticker, 10)
        );

        assertEquals("Client not found: 999", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "FAKE";
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(clientId, ticker, 10)
        );

        assertEquals("No market data available for ticker: FAKE", exception.getMessage());
        verify(ordersRepository, never()).createOrder(any());
    }

    @Test
    void placeBuyOrderThrowsWhenQuantityIsNegative() {
        String ticker = "AAPL";
        BigDecimal askPrice = new BigDecimal("150.00");
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, askPrice, new BigDecimal("149.00")))
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(1L)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(1L, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(1L, ticker, -5)
        );

        assertEquals("Quantity must be greater than zero.", exception.getMessage());
    }

    @Test
    void placeBuyOrderThrowsWhenMarketPriceUnavailable() {
        String ticker = "AAPL";
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceDataWithoutAskPrice(ticker))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(1L, ticker, 10)
        );

        assertEquals("Ask price not available for ticker: AAPL", exception.getMessage());
    }

    @Test
    void placeBuyOrderThrowsWhenClientIdIsNull() {
        String ticker = "AAPL";
        BigDecimal askPrice = new BigDecimal("150.00");
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, askPrice, new BigDecimal("149.00")))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeBuyOrder(null, ticker, 10)
        );

        assertEquals("Client ID must be greater than zero.", exception.getMessage());
    }

    // ========== PLACE SELL ORDER TESTS ==========

    @Test
    void placeSellOrderCreatesOrderWhenAllValidationsPass() {
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 5;
        BigDecimal bidPrice = new BigDecimal("160.00");

        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(
                List.of(testPriceData(ticker, new BigDecimal("161.00"), bidPrice))
        );
        when(marketHoursService.isUsMarketHours()).thenReturn(true);
        when(clientsService.getClientProfile(clientId)).thenReturn(
                new com.example.DTOs.clients.ClientProfileView(clientId, "user@test.com", "testuser", "Test", "User", new BigDecimal("5000.00"))
        );
        when(instrumentService.getInstrumentByTicker(ticker)).thenReturn(instrument(ticker, "STOCK"));
        when(ordersRepository.getPendingSellOrdersForClientAndTicker(clientId, ticker.toUpperCase()))
                .thenReturn(Collections.emptyList());
        when(holdingsService.getHolding(clientId, ticker.toUpperCase())).thenReturn(
                new com.example.DTOs.holdings.HoldingResponse(clientId, ticker, 10)
        );
        when(ordersRepository.createOrder(any(Order.class))).thenReturn(
                order(2L, clientId, ticker, Order.OrderType.SELL, quantity, bidPrice)
        );

        Order result = ordersService.placeSellOrder(clientId, ticker, quantity);

        assertNotNull(result);
        assertEquals(Order.OrderType.SELL, result.getOrderType());
        verify(marketDataService).getLatestPrice(ticker.toUpperCase());
        verify(clientsService).getClientProfile(clientId);
        verify(instrumentService).getInstrumentByTicker(ticker);
    }

    @Test
    void placeSellOrderThrowsWhenInstrumentDoesNotExist() {
        Long clientId = 1L;
        String ticker = "INVALID";
        
        when(marketDataService.getLatestPrice(ticker.toUpperCase())).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ordersService.placeSellOrder(clientId, ticker, 5)
        );

        assertEquals("No market data available for ticker: INVALID", exception.getMessage());
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

    /**
     * Creates test market price data Map with ask and bid prices.
     * Mimics the structure returned by MarketDataService.getLatestPrice()
     */
    private Map<String, Object> testPriceData(
            String ticker,
            BigDecimal askPrice,
            BigDecimal bidPrice) {
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("ticker", ticker);
        priceData.put("ask_price", askPrice);
        priceData.put("bid_price", bidPrice);
        priceData.put("ask_size", 100);
        priceData.put("bid_size", 100);
        priceData.put("ask_exchange", "NASDAQ");
        priceData.put("bid_exchange", "NASDAQ");
        priceData.put("tape", "C");
        priceData.put("quote_timestamp", System.currentTimeMillis());
        priceData.put("recorded_at", System.currentTimeMillis());
        priceData.put("asset_type", "STOCK");
        return priceData;
    }

    /**
     * Creates test market price data Map without ask_price (for error testing).
     */
    private Map<String, Object> testPriceDataWithoutAskPrice(String ticker) {
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("ticker", ticker);
        priceData.put("bid_price", new BigDecimal("149.00"));
        // ask_price is missing
        return priceData;
    }
}
