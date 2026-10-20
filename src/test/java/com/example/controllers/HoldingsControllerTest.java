package com.example.controllers;

import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.authUtils.ClientIdContextHolder;
import com.example.services.HoldingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingsControllerTest {
    private static final Long CLIENT_ID = 1L;

    @Mock
    private HoldingsService holdingsService;

    @InjectMocks
    private HoldingsController holdingsController;

    @BeforeEach
    void setUpContext() {
        ClientIdContextHolder.setClientId(CLIENT_ID);
    }

    @AfterEach
    void clearContext() {
        ClientIdContextHolder.clear();
    }

    @Test
    void getHoldingUsesAuthenticatedClient() {
        HoldingResponse expected = new HoldingResponse(CLIENT_ID, "AAPL", 50);
        when(holdingsService.getHolding(CLIENT_ID, "AAPL")).thenReturn(expected);

        assertEquals(expected, holdingsController.getHolding("AAPL"));
        verify(holdingsService).getHolding(CLIENT_ID, "AAPL");
    }

    @Test
    void getClientHoldingsUsesAuthenticatedClient() {
        List<HoldingResponse> expected = List.of(new HoldingResponse(CLIENT_ID, "AAPL", 50));
        when(holdingsService.getAllClientHoldings(CLIENT_ID)).thenReturn(expected);

        assertEquals(expected, holdingsController.getClientHoldings());
        verify(holdingsService).getAllClientHoldings(CLIENT_ID);
    }

    @Test
    void getQuantityUsesAuthenticatedClient() {
        QuantityResponse expected = new QuantityResponse(50);
        when(holdingsService.getQuantity(CLIENT_ID, "AAPL")).thenReturn(expected);

        assertEquals(expected, holdingsController.getQuantity("AAPL"));
        verify(holdingsService).getQuantity(CLIENT_ID, "AAPL");
    }
}
