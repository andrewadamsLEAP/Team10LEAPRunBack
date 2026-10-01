package com.example.controllers;

import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
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
    void getHoldingReturnsHoldingResponseWhenExists() {
        Long clientId = 1L;
        String ticker = "AAPL";
        HoldingResponse expected = new HoldingResponse(clientId, ticker, 50);

        when(holdingsService.getHolding(clientId, ticker)).thenReturn(expected);

        HoldingResponse result = holdingsController.getHolding(clientId, ticker);

        assertNotNull(result);
        assertEquals(expected, result);
        assertEquals(ticker, result.ticker());
        assertEquals(clientId, result.clientId());
        assertEquals(50, result.quantity());
        verify(holdingsService, times(1)).getHolding(clientId, ticker);
    }

    @Test
    void getHoldingReturnsNullWhenDoesNotExist() {
        Long clientId = 1L;
        String ticker = "FAKE";

        when(holdingsService.getHolding(clientId, ticker)).thenReturn(null);

        HoldingResponse result = holdingsController.getHolding(clientId, ticker);

        assertNull(result);
        verify(holdingsService).getHolding(clientId, ticker);
    }

    @Test
    void getHoldingReturnsCorrectValues() {
        Long clientId = 2L;
        String ticker = "MSFT";
        Integer quantity = 100;
        HoldingResponse expected = new HoldingResponse(clientId, ticker, quantity);

        when(holdingsService.getHolding(clientId, ticker)).thenReturn(expected);

        HoldingResponse result = holdingsController.getHolding(clientId, ticker);

        assertEquals(clientId, result.clientId());
        assertEquals(ticker, result.ticker());
        assertEquals(quantity, result.quantity());
    }

    // ========== GET CLIENT HOLDINGS TESTS ==========

    @Test
    void getClientHoldingsReturnsListOfHoldingResponses() {
        Long clientId = 1L;
        HoldingResponse aapl = new HoldingResponse(clientId, "AAPL", 50);
        HoldingResponse googl = new HoldingResponse(clientId, "GOOGL", 30);
        HoldingResponse msft = new HoldingResponse(clientId, "MSFT", 20);
        List<HoldingResponse> expected = List.of(aapl, googl, msft);

        when(holdingsService.getAllClientHoldings(clientId)).thenReturn(expected);

        List<HoldingResponse> result = holdingsController.getClientHoldings(clientId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("AAPL", result.get(0).ticker());
        assertEquals("GOOGL", result.get(1).ticker());
        assertEquals("MSFT", result.get(2).ticker());
        verify(holdingsService, times(1)).getAllClientHoldings(clientId);
    }

    @Test
    void getClientHoldingsReturnsEmptyListWhenClientHasNoHoldings() {
        Long clientId = 1L;

        when(holdingsService.getAllClientHoldings(clientId)).thenReturn(List.of());

        List<HoldingResponse> result = holdingsController.getClientHoldings(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(holdingsService).getAllClientHoldings(clientId);
    }

    @Test
    void getClientHoldingsReturnsSingleHoldingAsList() {
        Long clientId = 1L;
        HoldingResponse single = new HoldingResponse(clientId, "TSLA", 10);
        List<HoldingResponse> expected = List.of(single);

        when(holdingsService.getAllClientHoldings(clientId)).thenReturn(expected);

        List<HoldingResponse> result = holdingsController.getClientHoldings(clientId);

        assertEquals(1, result.size());
        assertEquals("TSLA", result.get(0).ticker());
        assertEquals(10, result.get(0).quantity());
    }

    @Test
    void getClientHoldingsReturnsMultipleDifferentStocks() {
        Long clientId = 2L;
        List<HoldingResponse> expected = List.of(
                new HoldingResponse(clientId, "AAPL", 25),
                new HoldingResponse(clientId, "MSFT", 15),
                new HoldingResponse(clientId, "AMZN", 40),
                new HoldingResponse(clientId, "NVDA", 5)
        );

        when(holdingsService.getAllClientHoldings(clientId)).thenReturn(expected);

        List<HoldingResponse> result = holdingsController.getClientHoldings(clientId);

        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(h -> h.ticker().equals("AAPL")));
        assertTrue(result.stream().anyMatch(h -> h.ticker().equals("MSFT")));
        assertTrue(result.stream().anyMatch(h -> h.ticker().equals("AMZN")));
        assertTrue(result.stream().anyMatch(h -> h.ticker().equals("NVDA")));
    }

    @Test
    void getClientHoldingsReturnsImmutableList() {
        Long clientId = 1L;
        HoldingResponse holding = new HoldingResponse(clientId, "AAPL", 50);
        List<HoldingResponse> holdings = List.of(holding);

        when(holdingsService.getAllClientHoldings(clientId)).thenReturn(holdings);

        List<HoldingResponse> result = holdingsController.getClientHoldings(clientId);

        // Verify we can't modify the returned list
        assertThrows(UnsupportedOperationException.class, 
                () -> result.add(new HoldingResponse(clientId, "GOOGL", 30)));
    }

    // ========== GET QUANTITY TESTS ==========

    @Test
    void getQuantityReturnsQuantityResponseWhenHoldingExists() {
        Long clientId = 1L;
        String ticker = "AAPL";
        QuantityResponse expected = new QuantityResponse(100);

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(expected);

        QuantityResponse result = holdingsController.getQuantity(clientId, ticker);

        assertNotNull(result);
        assertEquals(expected, result);
        assertEquals(100, result.quantity());
        verify(holdingsService).getQuantity(clientId, ticker);
    }

    @Test
    void getQuantityReturnsZeroWhenClientHasNoShares() {
        Long clientId = 1L;
        String ticker = "AAPL";
        QuantityResponse expected = new QuantityResponse(0);

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(expected);

        QuantityResponse result = holdingsController.getQuantity(clientId, ticker);

        assertEquals(0, result.quantity());
    }

    @Test
    void getQuantityReturnsNullWhenHoldingDoesNotExist() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(null);

        QuantityResponse result = holdingsController.getQuantity(clientId, ticker);

        assertNull(result);
    }

    @Test
    void getQuantityReturnsCorrectValues() {
        Long clientId = 2L;
        String ticker = "MSFT";
        Integer quantity = 75;
        QuantityResponse expected = new QuantityResponse(quantity);

        when(holdingsService.getQuantity(clientId, ticker)).thenReturn(expected);

        QuantityResponse result = holdingsController.getQuantity(clientId, ticker);

        assertEquals(quantity, result.quantity());
    }

}
