package com.example.services;

import com.example.DTOs.holdings.BuyStockResponse;
import com.example.DTOs.holdings.HoldingDtoConverter;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.DTOs.holdings.SellStockResponse;
import com.example.entities.Client;
import com.example.entities.Holding;
import com.example.entities.Order;
import com.example.repositories.ClientsRepository;
import com.example.repositories.HoldingsRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldingsServiceTest {

    private HoldingsRepository holdingsRepository;
    private ClientsRepository clientsRepository;
    private HoldingDtoConverter holdingDtoConverter;
    private ClientsService clientsService;
    private HoldingsService holdingsService;

    @BeforeEach
    void setUp() {
        holdingsRepository = mock(HoldingsRepository.class);
        clientsRepository = mock(ClientsRepository.class);
        holdingDtoConverter = new HoldingDtoConverter();
        clientsService = mock(ClientsService.class);
        holdingsService = new HoldingsService(holdingsRepository, clientsRepository, holdingDtoConverter, clientsService);
    }

    @Test
    void getHoldingReturnsHoldingResponseWhenValid() {
        Holding holding = holding(1L, "AAPL", 100);
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getHoldingsByClientAndTicker(1L, "AAPL")).thenReturn(holding);

        HoldingResponse response = holdingsService.getHolding(1L, "AAPL");

        assertNotNull(response);
        assertEquals(1L, response.clientId());
        assertEquals("AAPL", response.ticker());
        assertEquals(100, response.quantity());
    }

    @Test
    void getHoldingThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> holdingsService.getHolding(999L, "AAPL"));
    }

    @Test
    void getHoldingThrowsInvalidTickerWhenTickerEmpty() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        assertThrows(RuntimeException.class,
                () -> holdingsService.getHolding(1L, ""));
    }

    // ==== GET QUANTITY TESTS ====
    @Test
    void getQuantityReturnsQuantityResponseWhenValid() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(50);

        QuantityResponse response = holdingsService.getQuantity(1L, "AAPL");

        assertNotNull(response);
        assertEquals(50, response.quantity());
    }

    @Test
    void getQuantityReturnsZeroWhenNoHolding() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "UNKNOWN")).thenReturn(null);

        QuantityResponse response = holdingsService.getQuantity(1L, "UNKNOWN");

        assertNotNull(response);
        assertNull(response.quantity());
    }

    // ==== GET ALL CLIENT HOLDINGS TESTS ====
    @Test
    void getAllClientHoldingsReturnsListWhenPresent() {
        List<Holding> holdings = List.of(
                holding(1L, "AAPL", 100),
                holding(1L, "MSFT", 50)
        );
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getHoldingsByClient(1L)).thenReturn(holdings);

        List<HoldingResponse> responses = holdingsService.getAllClientHoldings(1L);

        assertEquals(2, responses.size());
        assertEquals("AAPL", responses.get(0).ticker());
        assertEquals("MSFT", responses.get(1).ticker());
    }

    @Test
    void getAllClientHoldingsReturnsEmptyListWhenNoHoldings() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getHoldingsByClient(1L)).thenReturn(List.of());

        List<HoldingResponse> responses = holdingsService.getAllClientHoldings(1L);

        assertEquals(0, responses.size());
    }

    // ==== BUY STOCK TESTS ====
    @Test
    void buyStockCreatesNewHoldingWhenFirstPurchase() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(null);

        BuyStockResponse response = holdingsService.buyStock(1L, "AAPL", 100);

        assertNotNull(response);
        assertEquals(1L, response.clientId());
        assertEquals("AAPL", response.ticker());
        assertEquals(100, response.newQuantity());
        assertEquals("Stock purchased successfully", response.message());
        verify(holdingsRepository).createHolding(any(Holding.class));
    }

    @Test
    void buyStockCreatesNewHoldingWhenZeroQuantity() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(0);

        BuyStockResponse response = holdingsService.buyStock(1L, "AAPL", 100);

        assertNotNull(response);
        assertEquals(100, response.newQuantity());
    }

    @Test
    void buyStockIncreasesExistingHolding() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(100);
        when(holdingsRepository.updateBuyHolding(50, 1L, "AAPL")).thenReturn(1);

        BuyStockResponse response = holdingsService.buyStock(1L, "AAPL", 50);

        assertNotNull(response);
        assertEquals(150, response.newQuantity());
        verify(holdingsRepository).updateBuyHolding(50, 1L, "AAPL");
    }

    // ==== SELL STOCK TESTS ====
    @Test
    void sellStockReducesExistingHolding() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(100);
        when(holdingsRepository.updateSellHolding(50, 1L, "AAPL")).thenReturn(1);

        SellStockResponse response = holdingsService.sellStock(1L, "AAPL", 50);

        assertNotNull(response);
        assertEquals(1L, response.clientId());
        assertEquals("AAPL", response.ticker());
        assertEquals(50, response.newQuantity());
        assertEquals("Stock sold successfully", response.message());
        verify(holdingsRepository).updateSellHolding(50, 1L, "AAPL");
    }

    @Test
    void sellStockThrowsInsufficientSharesWhenNothingOwned() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 10));
    }

    @Test
    void sellStockThrowsInsufficientSharesWhenNotEnough() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(50);

        assertThrows(IllegalArgumentException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 100));
    }

    @Test
    void sellStockThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> holdingsService.sellStock(999L, "AAPL", 50));
    }

    @Test
    void sellStockThrowsInvalidQuantityWhenNegative() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        assertThrows(RuntimeException.class,
                () -> holdingsService.sellStock(1L, "AAPL", -10));
    }

    @Test
    void sellStockThrowsInvalidQuantityWhenZero() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        assertThrows(RuntimeException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 0));
    }

    // ==== UPDATE HOLDINGS FOR ORDER TESTS ====
    @Test
    void updateHoldingsForOrderBuyExecutesWhenBuyOrder() {
        Order buyOrder = new Order();
        buyOrder.setOrderType(Order.OrderType.BUY);
        buyOrder.setClientId(1L);
        buyOrder.setTicker("AAPL");
        buyOrder.setQuantity(100);
        buyOrder.setPrice(new BigDecimal("150.00"));

        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(null);
        when(clientsService.updateCashAmount(1L, new BigDecimal("-15000.00"))).thenReturn(new BigDecimal("0.00"));

        holdingsService.updateHoldingsForOrder(buyOrder);

        verify(holdingsRepository).createHolding(any(Holding.class));
    }

    @Test
    void updateHoldingsForOrderSellExecutesWhenSellOrder() {
        Order sellOrder = new Order();
        sellOrder.setOrderType(Order.OrderType.SELL);
        sellOrder.setClientId(1L);
        sellOrder.setTicker("AAPL");
        sellOrder.setQuantity(50);
        sellOrder.setPrice(new BigDecimal("160.00"));

        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(100);
        when(holdingsRepository.updateSellHolding(50, 1L, "AAPL")).thenReturn(1);
        when(clientsService.updateCashAmount(1L, new BigDecimal("8000.00"))).thenReturn(new BigDecimal("0.00"));

        holdingsService.updateHoldingsForOrder(sellOrder);

        verify(holdingsRepository).updateSellHolding(50, 1L, "AAPL");
    }

    private Holding holding(Long clientId, String ticker, Integer quantity) {
        Holding holding = new Holding();
        holding.setClient_Id(clientId);
        holding.setTicker(ticker);
        holding.setQuantity(quantity);
        return holding;
    }

    private Client client(Long clientId) {
        Client client = new Client();
        client.setClientId(clientId);
        return client;
    }
}
