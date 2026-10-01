package com.example.controllers;

import com.example.entities.Holding;
import com.example.services.HoldingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HoldingsController.
 * Tests the controller layer with mocked HoldingsService.
 * Focuses on testing List<Holding> handling from the fix.
 */
@ExtendWith(MockitoExtension.class)
class HoldingsControllerTest {

    @Mock
    private HoldingsService holdingsService;

    private HoldingsController holdingsController;

    @BeforeEach
    void setUp() {
        holdingsController = new HoldingsController(holdingsService);
    }

    // ========== TEST ENDPOINT TESTS ==========

    @Test
    void testTestEndpointReturnsSuccessMessage() {
        String result = holdingsController.test();

        assertEquals("Test endpoint works!", result);
        verifyNoInteractions(holdingsService);
    }

    @Test
    void testServiceTestEndpointCallsService() {
        when(holdingsService.test()).thenReturn("Test service works!");

        String result = holdingsController.serviceTest();

        assertEquals("Test service works!", result);
        verify(holdingsService, times(1)).test();
    }

    // ========== GET SPECIFIC HOLDING TESTS ==========

    @Test
    void getHoldingReturnsHoldingWhenExists() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Holding expected = holding(clientId, ticker, 50);

        when(holdingsService.getHolding(clientId, ticker)).thenReturn(expected);

        Holding result = holdingsController.getHolding(clientId, ticker);

        assertNotNull(result);
        assertEquals(expected, result);
        assertEquals(ticker, result.getTicker());
        verify(holdingsService, times(1)).getHolding(clientId, ticker);
    }

    @Test
    void getHoldingReturnsNullWhenDoesNotExist() {
        Long clientId = 1L;
        String ticker = "FAKE";

        when(holdingsService.getHolding(clientId, ticker)).thenReturn(null);

        Holding result = holdingsController.getHolding(clientId, ticker);

        assertNull(result);
        verify(holdingsService).getHolding(clientId, ticker);
    }

    // ========== GET CLIENT HOLDINGS TESTS (KEY FIX) ==========

    @Test
    void getClientHoldingsReturnsListOfAllHoldings() {
        Long clientId = 1L;
        Holding aapl = holding(clientId, "AAPL", 50);
        Holding googl = holding(clientId, "GOOGL", 30);
        Holding msft = holding(clientId, "MSFT", 20);
        List<Holding> expected = List.of(aapl, googl, msft);

        when(holdingsService.getClientHoldings(clientId)).thenReturn(expected);

        List<Holding> result = holdingsController.getClientHoldings(clientId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expected, result);
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("GOOGL", result.get(1).getTicker());
        assertEquals("MSFT", result.get(2).getTicker());
        verify(holdingsService, times(1)).getClientHoldings(clientId);
    }

    @Test
    void getClientHoldingsReturnsEmptyListWhenClientHasNoHoldings() {
        Long clientId = 1L;

        when(holdingsService.getClientHoldings(clientId)).thenReturn(List.of());

        List<Holding> result = holdingsController.getClientHoldings(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(holdingsService).getClientHoldings(clientId);
    }

    @Test
    void getClientHoldingsReturnsSingleHoldingAsList() {
        Long clientId = 1L;
        Holding single = holding(clientId, "TSLA", 10);
        List<Holding> expected = List.of(single);

        when(holdingsService.getClientHoldings(clientId)).thenReturn(expected);

        List<Holding> result = holdingsController.getClientHoldings(clientId);

        assertEquals(1, result.size());
        assertEquals("TSLA", result.get(0).getTicker());
        assertEquals(10, result.get(0).getQuantity());
    }

    @Test
    void getClientHoldingsReturnsMultipleDifferentStocks() {
        Long clientId = 2L;
        List<Holding> expected = List.of(
                holding(clientId, "AAPL", 25),
                holding(clientId, "MSFT", 15),
                holding(clientId, "AMZN", 40),
                holding(clientId, "NVDA", 5)
        );

        when(holdingsService.getClientHoldings(clientId)).thenReturn(expected);

        List<Holding> result = holdingsController.getClientHoldings(clientId);

        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(h -> h.getTicker().equals("AAPL")));
        assertTrue(result.stream().anyMatch(h -> h.getTicker().equals("MSFT")));
        assertTrue(result.stream().anyMatch(h -> h.getTicker().equals("AMZN")));
        assertTrue(result.stream().anyMatch(h -> h.getTicker().equals("NVDA")));
    }

    @Test
    void getClientHoldingsReturnsImmutableList() {
        Long clientId = 1L;
        Holding holding = holding(clientId, "AAPL", 50);
        List<Holding> holdings = List.of(holding);

        when(holdingsService.getClientHoldings(clientId)).thenReturn(holdings);

        List<Holding> result = holdingsController.getClientHoldings(clientId);

        // Verify we can't modify the returned list
        assertThrows(UnsupportedOperationException.class, () -> result.add(holding(clientId, "GOOGL", 30)));
    }

    // ========== GET QUANTITY TESTS ==========

    @Test
    void getQuantityReturnsQuantityWhenHoldingExists() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Integer expected = 100;

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(expected);

        Integer result = holdingsController.getQuantity(clientId, ticker);

        assertEquals(expected, result);
        verify(holdingsService).getQuantity(clientId, ticker);
    }

    @Test
    void getQuantityReturnsZeroWhenClientHasNoShares() {
        Long clientId = 1L;
        String ticker = "AAPL";

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(0);

        Integer result = holdingsController.getQuantity(clientId, ticker);

        assertEquals(0, result);
    }

    @Test
    void getQuantityReturnsNullWhenHoldingDoesNotExist() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(null);

        Integer result = holdingsController.getQuantity(clientId, ticker);

        assertNull(result);
    }

    // ========== HELPER METHODS ==========

    private Holding holding(Long clientId, String ticker, int quantity) {
        Holding holding = new Holding();
        holding.setClient_Id(clientId);
        holding.setTicker(ticker);
        holding.setQuantity(quantity);
        return holding;
    }
}
