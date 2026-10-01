package com.example.repositories;

import com.example.entities.Holding;
import com.example.mappers.HoldingsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingsRepositoryTest {

    @Mock
    private HoldingsMapper holdingsMapper;

    @InjectMocks
    private HoldingsRepository holdingsRepository;

    /**
     * Tests getting holding by client and ticker
     */
    @Test
    void getHoldingsByClientAndTicker_shouldReturnHoldingFromMapper() {
        Long clientId = 1L;
        String ticker = "AAPL";
        Holding mockHolding = new Holding();
        mockHolding.setClient_Id(clientId);
        mockHolding.setTicker(ticker);
        mockHolding.setQuantity(100);

        when(holdingsMapper.getHoldingsByClientAndTicker(clientId, ticker)).thenReturn(mockHolding);

        Holding result = holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker);

        assertNotNull(result);
        assertEquals(clientId, result.getClient_Id());
        assertEquals(ticker, result.getTicker());
        assertEquals(100, result.getQuantity());
        verify(holdingsMapper, times(1)).getHoldingsByClientAndTicker(clientId, ticker);
    }

    /**
     * Tests getting quantity by client and ticker
     */
    @Test
    void getQuantityByClientAndTicker_shouldReturnQuantityFromMapper() {
        Long clientId = 1L;
        String ticker = "GOOGL";

        when(holdingsMapper.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(50);

        Integer result = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);

        assertNotNull(result);
        assertEquals(50, result);
        verify(holdingsMapper, times(1)).getQuantityByClientAndTicker(clientId, ticker);
    }

    /**
     * Tests getting holdings by client
     */
    @Test
    void getHoldingsByClient_shouldReturnListOfHoldingsForClient() {
        Long clientId = 1L;
        List<Holding> mockHoldings = new ArrayList<>();
        
        Holding holding1 = new Holding();
        holding1.setClient_Id(clientId);
        holding1.setTicker("AAPL");
        holding1.setQuantity(100);
        mockHoldings.add(holding1);

        Holding holding2 = new Holding();
        holding2.setClient_Id(clientId);
        holding2.setTicker("GOOGL");
        holding2.setQuantity(50);
        mockHoldings.add(holding2);

        when(holdingsMapper.getHoldingsByClient(clientId)).thenReturn(mockHoldings);

        List<Holding> result = holdingsRepository.getHoldingsByClient(clientId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("GOOGL", result.get(1).getTicker());
        verify(holdingsMapper, times(1)).getHoldingsByClient(clientId);
    }

    /**
     * Tests creating holding
     */
    @Test
    void createHolding_shouldReturnCreatedHolding() {
        Holding holdingToCreate = new Holding();
        holdingToCreate.setClient_Id(1L);
        holdingToCreate.setTicker("MSFT");
        holdingToCreate.setQuantity(75);

        doNothing().when(holdingsMapper).createHolding(any(Holding.class));

        Holding result = holdingsRepository.createHolding(holdingToCreate);

        assertNotNull(result);
        assertEquals(1L, result.getClient_Id());
        assertEquals("MSFT", result.getTicker());
        verify(holdingsMapper, times(1)).createHolding(any(Holding.class));
    }

    /**
     * Tests updating buy holding
     */
    @Test
    void updateBuyHolding_shouldUpdateAndReturnAffectedRows() {
        Integer quantity = 50;
        Long clientId = 1L;
        String ticker = "AAPL";

        when(holdingsMapper.updateBuyHolding(quantity, clientId, ticker)).thenReturn(1);

        int result = holdingsRepository.updateBuyHolding(quantity, clientId, ticker);

        assertEquals(1, result);
        verify(holdingsMapper, times(1)).updateBuyHolding(quantity, clientId, ticker);
    }

    /**
     * Tests updating sell holding
     */
    @Test
    void updateSellHolding_shouldUpdateAndReturnAffectedRows() {
        Integer quantity = 30;
        Long clientId = 1L;
        String ticker = "GOOGL";

        when(holdingsMapper.updateSellHolding(quantity, clientId, ticker)).thenReturn(1);

        int result = holdingsRepository.updateSellHolding(quantity, clientId, ticker);

        assertEquals(1, result);
        verify(holdingsMapper, times(1)).updateSellHolding(quantity, clientId, ticker);
    }

    /**
     * Tests getting holdings returns empty list
     */
    @Test
    void getHoldingsByClient_shouldReturnEmptyListWhenNoHoldings() {
        Long clientId = 1L;

        when(holdingsMapper.getHoldingsByClient(clientId)).thenReturn(new ArrayList<>());

        List<Holding> result = holdingsRepository.getHoldingsByClient(clientId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests getting quantity returns null when no holding
     */
    @Test
    void getQuantityByClientAndTicker_shouldReturnNullWhenNoHolding() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsMapper.getQuantityByClientAndTicker(clientId, ticker)).thenReturn(null);

        Integer result = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);

        assertNull(result);
    }

    /**
     * Tests getting holding returns null when not found
     */
    @Test
    void getHoldingsByClientAndTicker_shouldReturnNullWhenNotFound() {
        Long clientId = 1L;
        String ticker = "NONEXISTENT";

        when(holdingsMapper.getHoldingsByClientAndTicker(clientId, ticker)).thenReturn(null);

        Holding result = holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker);

        assertNull(result);
    }

    /**
     * Tests updating buy holding with large quantity
     */
    @Test
    void updateBuyHolding_shouldHandleLargeQuantities() {
        Integer quantity = 999999;
        Long clientId = 1L;
        String ticker = "AAPL";

        when(holdingsMapper.updateBuyHolding(quantity, clientId, ticker)).thenReturn(1);

        int result = holdingsRepository.updateBuyHolding(quantity, clientId, ticker);

        assertEquals(1, result);
    }

    /**
     * Tests update returns zero when holding not found
     */
    @Test
    void updateSellHolding_shouldReturnZeroWhenHoldingNotFound() {
        Integer quantity = 50;
        Long clientId = 999L;
        String ticker = "NONEXISTENT";

        when(holdingsMapper.updateSellHolding(quantity, clientId, ticker)).thenReturn(0);

        int result = holdingsRepository.updateSellHolding(quantity, clientId, ticker);

        assertEquals(0, result);
    }
}
