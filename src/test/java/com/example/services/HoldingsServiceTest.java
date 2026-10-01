package com.example.services;

import com.example.DTOs.holdings.BuyStockResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.DTOs.holdings.SellStockResponse;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.HoldingDtoConverter;
import com.example.entities.Holding;
import com.example.entities.Order;
import com.example.exceptions.ClientNotFoundException;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.ClientsRepository;
import com.example.repositories.HoldingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldingsServiceTest {

    private HoldingsRepository holdingsRepository;
    private ClientsRepository clientsRepository;
    private HoldingDtoConverter holdingDtoConverter;
    private HoldingsService holdingsService;

    @BeforeEach
    void setUp() {
        holdingsRepository = mock(HoldingsRepository.class);
        clientsRepository = mock(ClientsRepository.class);
        holdingDtoConverter = new HoldingDtoConverter();
        holdingsService = new HoldingsService(holdingsRepository, clientsRepository, holdingDtoConverter);
    }

    // ==== GET HOLDING TESTS ====
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

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> holdingsService.getHolding(999L, "AAPL"));

        assertEquals("Client with ID 999 not found in or has no holdings", exception.getMessage());
    }

    @Test
    void getHoldingThrowsInvalidTickerWhenTickerEmpty() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.getHolding(1L, ""));

        assertEquals("Invalid Ticker: Ticker cannot be null or empty: ", exception.getMessage());
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
        assertEquals(null, response.quantity());
    }

    @Test
    void getQuantityThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> holdingsService.getQuantity(999L, "AAPL"));

        assertEquals("Client with ID 999 not found in or has no holdings", exception.getMessage());
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

    @Test
    void getAllClientHoldingsThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> holdingsService.getAllClientHoldings(999L));

        assertEquals("Client with ID 999 not found in or has no holdings", exception.getMessage());
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

    @Test
    void buyStockThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> holdingsService.buyStock(999L, "AAPL", 100));

        assertEquals("Client with ID 999 not found in or has no holdings", exception.getMessage());
    }

    @Test
    void buyStockThrowsInvalidQuantityWhenNegative() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.buyStock(1L, "AAPL", -10));

        assertEquals("Invalid Quantity: Quantity must be positive: -10", exception.getMessage());
    }

    @Test
    void buyStockThrowsInvalidQuantityWhenZeroQuantity() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.buyStock(1L, "AAPL", 0));

        assertEquals("Invalid Quantity: Quantity must be positive: 0", exception.getMessage());
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

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 10));

        assertEquals("Invalid Sell Operation: Insufficient shares to sell. Current: 0, Trying to sell: 10", exception.getMessage());
    }

    @Test
    void sellStockThrowsInsufficientSharesWhenNotEnough() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(50);

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 100));

        assertEquals("Invalid Sell Operation: Insufficient shares to sell. Current: 50, Trying to sell: 100", exception.getMessage());
    }

    @Test
    void sellStockThrowsClientNotFoundWhenClientNotFound() {
        when(clientsRepository.findClientById(999L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class,
                () -> holdingsService.sellStock(999L, "AAPL", 50));

        assertEquals("Client with ID 999 not found in or has no holdings", exception.getMessage());
    }

    @Test
    void sellStockThrowsInvalidQuantityWhenNegative() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.sellStock(1L, "AAPL", -10));

        assertEquals("Invalid Quantity: Quantity must be positive: -10", exception.getMessage());
    }

    @Test
    void sellStockThrowsInvalidQuantityWhenZero() {
        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));

        InvalidArgumentsException exception = assertThrows(InvalidArgumentsException.class,
                () -> holdingsService.sellStock(1L, "AAPL", 0));

        assertEquals("Invalid Quantity: Quantity must be positive: 0", exception.getMessage());
    }

    // ==== UPDATE HOLDINGS FOR ORDER TESTS ====
    @Test
    void updateHoldingsForOrderBuyExecutesWhenBuyOrder() {
        Order buyOrder = mock(Order.class);
        when(buyOrder.orderType()).thenReturn(Order.OrderType.BUY);
        when(buyOrder.clientId()).thenReturn(1L);
        when(buyOrder.ticker()).thenReturn("AAPL");
        when(buyOrder.quantity()).thenReturn(100);

        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(null);

        holdingsService.updateHoldingsForOrder(buyOrder);

        verify(holdingsRepository).createHolding(any(Holding.class));
    }

    @Test
    void updateHoldingsForOrderSellExecutesWhenSellOrder() {
        Order sellOrder = mock(Order.class);
        when(sellOrder.orderType()).thenReturn(Order.OrderType.SELL);
        when(sellOrder.clientId()).thenReturn(1L);
        when(sellOrder.ticker()).thenReturn("AAPL");
        when(sellOrder.quantity()).thenReturn(50);

        when(clientsRepository.findClientById(1L)).thenReturn(client(1L));
        when(holdingsRepository.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(100);
        when(holdingsRepository.updateSellHolding(50, 1L, "AAPL")).thenReturn(1);

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

    private com.example.entities.Client client(Long clientId) {
        com.example.entities.Client client = new com.example.entities.Client();
        client.setClientId(clientId);
        return client;
    }
}
