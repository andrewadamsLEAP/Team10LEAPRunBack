package com.example.repositories;

import com.example.entities.Holding;
import com.example.mappers.HoldingsMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldingsRepositoryTest {

    @Test
    void getHoldingsByClientAndTickerDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        Holding expected = holding(1L, "AAPL", 100);
        when(mapper.getHoldingsByClientAndTicker(1L, "AAPL")).thenReturn(expected);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        Holding actual = repository.getHoldingsByClientAndTicker(1L, "AAPL");

        assertSame(expected, actual);
        verify(mapper).getHoldingsByClientAndTicker(1L, "AAPL");
    }

    @Test
    void getHoldingsByClientAndTickerReturnsNullWhenNotFound() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.getHoldingsByClientAndTicker(1L, "MISSING")).thenReturn(null);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        Holding actual = repository.getHoldingsByClientAndTicker(1L, "MISSING");

        assertNull(actual);
        verify(mapper).getHoldingsByClientAndTicker(1L, "MISSING");
    }

    @Test
    void getQuantityByClientAndTickerDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.getQuantityByClientAndTicker(1L, "AAPL")).thenReturn(50);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        Integer actual = repository.getQuantityByClientAndTicker(1L, "AAPL");

        assertEquals(50, actual);
        verify(mapper).getQuantityByClientAndTicker(1L, "AAPL");
    }

    @Test
    void getQuantityByClientAndTickerReturnsNullWhenNotFound() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.getQuantityByClientAndTicker(1L, "UNKNOWN")).thenReturn(null);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        Integer actual = repository.getQuantityByClientAndTicker(1L, "UNKNOWN");

        assertNull(actual);
    }

    @Test
    void getHoldingsByClientDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        List<Holding> expected = List.of(holding(1L, "AAPL", 100), holding(1L, "MSFT", 50));
        when(mapper.getHoldingsByClient(1L)).thenReturn(expected);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        List<Holding> actual = repository.getHoldingsByClient(1L);

        assertSame(expected, actual);
        assertEquals(2, actual.size());
        verify(mapper).getHoldingsByClient(1L);
    }

    @Test
    void getHoldingsByClientReturnsEmptyListWhenNoHoldings() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.getHoldingsByClient(2L)).thenReturn(List.of());
        HoldingsRepository repository = new HoldingsRepository(mapper);

        List<Holding> actual = repository.getHoldingsByClient(2L);

        assertEquals(0, actual.size());
    }

    @Test
    void createHoldingDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        Holding holding = holding(1L, "AAPL", 100);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        Holding actual = repository.createHolding(holding);

        assertSame(holding, actual);
        verify(mapper).createHolding(holding);
    }

    @Test
    void updateBuyHoldingDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.updateBuyHolding(50, 1L, "AAPL")).thenReturn(1);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        int actual = repository.updateBuyHolding(50, 1L, "AAPL");

        assertEquals(1, actual);
        verify(mapper).updateBuyHolding(50, 1L, "AAPL");
    }

    @Test
    void updateBuyHoldingReturnsZeroWhenUpdateFails() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.updateBuyHolding(50, 1L, "MISSING")).thenReturn(0);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        int actual = repository.updateBuyHolding(50, 1L, "MISSING");

        assertEquals(0, actual);
    }

    @Test
    void updateSellHoldingDelegatesToMapper() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.updateSellHolding(25, 1L, "AAPL")).thenReturn(1);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        int actual = repository.updateSellHolding(25, 1L, "AAPL");

        assertEquals(1, actual);
        verify(mapper).updateSellHolding(25, 1L, "AAPL");
    }

    @Test
    void updateSellHoldingReturnsZeroWhenUpdateFails() {
        HoldingsMapper mapper = mock(HoldingsMapper.class);
        when(mapper.updateSellHolding(25, 1L, "MISSING")).thenReturn(0);
        HoldingsRepository repository = new HoldingsRepository(mapper);

        int actual = repository.updateSellHolding(25, 1L, "MISSING");

        assertEquals(0, actual);
    }

    private Holding holding(Long clientId, String ticker, Integer quantity) {
        Holding holding = new Holding();
        holding.setClient_Id(clientId);
        holding.setTicker(ticker);
        holding.setQuantity(quantity);
        return holding;
    }
}
