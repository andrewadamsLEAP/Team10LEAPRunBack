package com.example.services;

import com.example.entities.Holding;
import com.example.entities.Order;
import com.example.repositories.HoldingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HoldingsServiceTest {

    private HoldingsRepository holdingsRepository;
    private HoldingsService holdingsService;

    @BeforeEach
    void setUp() {
        holdingsRepository = mock(HoldingsRepository.class);
        holdingsService = new HoldingsService(holdingsRepository);
    }

    // ========== GET HOLDING TESTS ==========

    @Test
    void getHoldingReturnsHoldingForClientAndTicker() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Holding expected = holding(clientId, ticker, 50);

        when(holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker)).thenReturn(expected);

        Holding result = holdingsService.getHolding(clientId, ticker);

        assertNotNull(result);
        assertEquals(expected, result);
        assertEquals(ticker, result.getTicker());
        assertEquals(50, result.getQuantity());
        verify(holdingsRepository).getHoldingsByClientAndTicker(clientId, ticker);
    }

    @Test
    void getHoldingReturnsNullWhenHoldingDoesNotExist() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker)).thenReturn(null);

        Holding result = holdingsService.getHolding(clientId, ticker);

        assertNull(result);
    }

    // ========== GET CLIENT HOLDINGS TESTS (LIST) ==========

    @Test
    void getClientHoldingsReturnsListOfAllHoldingsForClient() {
        Long clientId = 1L;
        Holding holding1 = holding(clientId, "AAPL", 50);
        Holding holding2 = holding(clientId, "GOOGL", 30);
        Holding holding3 = holding(clientId, "MSFT", 20);
        List<Holding> expected = List.of(holding1, holding2, holding3);

        when(holdingsRepository.getHoldingsByClient(clientId)).thenReturn(expected);

        List<Holding> result = holdingsService.getClientHoldings(clientId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expected, result);
        verify(holdingsRepository).getHoldingsByClient(clientId);
    }

    @Test
    void getClientHoldingsReturnsEmptyListWhenClientHasNoHoldings() {
        Long clientId = 1L;

        when(holdingsRepository.getHoldingsByClient(clientId)).thenReturn(List.of());

        List<Holding> result = holdingsService.getClientHoldings(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getClientHoldingsReturnsSingleHoldingInList() {
        Long clientId = 1L;
        Holding single = holding(clientId, "TSLA", 10);
        List<Holding> expected = List.of(single);

        when(holdingsRepository.getHoldingsByClient(clientId)).thenReturn(expected);

        List<Holding> result = holdingsService.getClientHoldings(clientId);

        assertEquals(1, result.size());
        assertEquals("TSLA", result.get(0).getTicker());
    }

    @Test
    void getClientHoldingsReturnsMultipleDifferentTickers() {
        Long clientId = 2L;
        Holding aapl = holding(clientId, "AAPL", 25);
        Holding msft = holding(clientId, "MSFT", 15);
        Holding amzn = holding(clientId, "AMZN", 40);
        Holding nvda = holding(clientId, "NVDA", 5);
        List<Holding> expected = List.of(aapl, msft, amzn, nvda);

        when(holdingsRepository.getHoldingsByClient(clientId)).thenReturn(expected);

        List<Holding> result = holdingsService.getClientHoldings(clientId);

        assertEquals(4, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("MSFT", result.get(1).getTicker());
        assertEquals("AMZN", result.get(2).getTicker());
        assertEquals("NVDA", result.get(3).getTicker());
    }

    // ========== GET QUANTITY TESTS ==========

    @Test
    void getQuantityReturnsQuantityWhenHoldingExists() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Integer expected = 100;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(expected);

        Integer result = holdingsService.getQuantity(clientId, ticker);

        assertEquals(expected, result);
        verify(holdingsRepository).getQuantityByClientAndTicker(clientId, ticker);
    }

    @Test
    void getQuantityReturnsNullWhenHoldingDoesNotExist() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(null);

        Integer result = holdingsService.getQuantity(clientId, ticker);

        assertNull(result);
    }

    @Test
    void getQuantityReturnsZeroWhenClientHasNoShares() {
        Long clientId = 1L;
        String ticker = "AAPL";

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(0);

        Integer result = holdingsService.getQuantity(clientId, ticker);

        assertEquals(0, result);
    }

    // ========== BUY STOCK TESTS ==========

    @Test
    void buyStockCreatesNewHoldingWhenClientDoesNotOwnTicker() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Integer quantity = 50;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(null);
        when(holdingsRepository.createHolding(any(Holding.class))).thenReturn(
                holding(clientId, ticker, quantity)
        );

        holdingsService.buyStock(clientId, ticker, quantity);

        verify(holdingsRepository).getQuantityByClientAndTicker(clientId, ticker);
        verify(holdingsRepository).createHolding(any(Holding.class));
        verify(holdingsRepository, never()).updateBuyHolding(anyInt(), anyLong(), anyString());
    }

    @Test
    void buyStockCreatesNewHoldingWhenQuantityIsZero() {
        Long clientId = 1L;
        String ticker = "GOOGL";
        Integer quantity = 30;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(0);
        when(holdingsRepository.createHolding(any(Holding.class))).thenReturn(
                holding(clientId, ticker, quantity)
        );

        holdingsService.buyStock(clientId, ticker, quantity);

        verify(holdingsRepository).createHolding(any(Holding.class));
        verify(holdingsRepository, never()).updateBuyHolding(anyInt(), anyLong(), anyString());
    }

    @Test
    void buyStockUpdatesExistingHoldingWhenClientAlreadyOwnsTicker() {
        Long clientId = 1L;
        String ticker = "MSFT";
        Integer currentQuantity = 25;
        Integer newQuantity = 15;
        Integer expectedTotal = 40;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(currentQuantity);
        when(holdingsRepository.updateBuyHolding(newQuantity, clientId, ticker)).thenReturn(1);

        holdingsService.buyStock(clientId, ticker, newQuantity);

        verify(holdingsRepository).getQuantityByClientAndTicker(clientId, ticker);
        verify(holdingsRepository, never()).createHolding(any());
        verify(holdingsRepository).updateBuyHolding(newQuantity, clientId, ticker);
    }

    // ========== SELL STOCK TESTS ==========

    @Test
    void sellStockDecreasesQuantityWhenClientOwnsSufficientShares() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Integer quantityToSell = 10;
        Integer currentQuantity = 50;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(currentQuantity);
        when(holdingsRepository.updateSellHolding(quantityToSell, clientId, ticker)).thenReturn(1);

        holdingsService.sellStock(clientId, ticker, quantityToSell);

        verify(holdingsRepository).updateSellHolding(quantityToSell, clientId, ticker);
    }

    @Test
    void sellStockThrowsWhenClientDoesNotOwnStock() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Integer quantityToSell = 10;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> holdingsService.sellStock(clientId, ticker, quantityToSell)
        );

        assertTrue(exception.getMessage().contains("Insufficient shares to sell"));
        assertTrue(exception.getMessage().contains("Current: 0"));
        verify(holdingsRepository, never()).updateSellHolding(anyInt(), anyLong(), anyString());
    }

    @Test
    void sellStockThrowsWhenClientDoesNotOwnSufficientShares() {
        Long clientId = 1L;
        String ticker = "GOOGL";
        Integer quantityToSell = 100;
        Integer currentQuantity = 50;

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(currentQuantity);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> holdingsService.sellStock(clientId, ticker, quantityToSell)
        );

        assertTrue(exception.getMessage().contains("Insufficient shares to sell"));
        assertTrue(exception.getMessage().contains("Current: 50"));
        assertTrue(exception.getMessage().contains("Trying to sell: 100"));
        verify(holdingsRepository, never()).updateSellHolding(anyInt(), anyLong(), anyString());
    }

    // ========== UPDATE HOLDINGS FOR ORDER TESTS ==========

    @Test
    void updateHoldingsForOrderBuyOrderIncreasesHoldings() {
        Long clientId = 1L;
        String ticker = "AAPL";
        int quantity = 25;
        Order buyOrder = createOrder(1L, clientId, ticker, Order.OrderType.BUY, quantity);

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(50);
        when(holdingsRepository.updateBuyHolding(quantity, clientId, ticker)).thenReturn(1);

        holdingsService.updateHoldingsForOrder(buyOrder);

        verify(holdingsRepository).updateBuyHolding(quantity, clientId, ticker);
        verify(holdingsRepository, never()).updateSellHolding(anyInt(), anyLong(), anyString());
    }

    @Test
    void updateHoldingsForOrderSellOrderDecreasesHoldings() {
        Long clientId = 1L;
        String ticker = "MSFT";
        int quantity = 10;
        Order sellOrder = createOrder(2L, clientId, ticker, Order.OrderType.SELL, quantity);

        when(holdingsRepository.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(50);
        when(holdingsRepository.updateSellHolding(quantity, clientId, ticker)).thenReturn(1);

        holdingsService.updateHoldingsForOrder(sellOrder);

        verify(holdingsRepository).updateSellHolding(quantity, clientId, ticker);
        verify(holdingsRepository, never()).updateBuyHolding(anyInt(), anyLong(), anyString());
    }

    // ========== HELPER METHODS ==========

    private Holding holding(Long clientId, String ticker, int quantity) {
        Holding holding = new Holding();
        holding.setClient_Id(clientId);
        holding.setTicker(ticker);
        holding.setQuantity(quantity);
        return holding;
    }

    private Order createOrder(Long orderId, Long clientId, String ticker, Order.OrderType orderType, int quantity) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setClientId(clientId);
        order.setTicker(ticker);
        order.setOrderType(orderType);
        order.setQuantity(quantity);
        return order;
    }
}
