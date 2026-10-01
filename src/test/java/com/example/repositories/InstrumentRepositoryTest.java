package com.example.repositories;

import com.example.entities.Instrument;
import com.example.mappers.InstrumentMapper;
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
class InstrumentRepositoryTest {

    @Mock
    private InstrumentMapper instrumentMapper;

    @InjectMocks
    private InstrumentRepository instrumentRepository;

    /**
     * Tests finding all instruments
     */
    @Test
    void findAll_shouldReturnAllInstrumentsFromMapper() {
        List<Instrument> mockInstruments = new ArrayList<>();
        
        Instrument inst1 = new Instrument();
        inst1.setTicker("AAPL");
        inst1.setAssetType("stock");
        mockInstruments.add(inst1);

        Instrument inst2 = new Instrument();
        inst2.setTicker("GOOGL");
        inst2.setAssetType("stock");
        mockInstruments.add(inst2);

        when(instrumentMapper.findAll()).thenReturn(mockInstruments);

        List<Instrument> result = instrumentRepository.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("GOOGL", result.get(1).getTicker());
        verify(instrumentMapper, times(1)).findAll();
    }

    /**
     * Tests finding instrument by ticker
     */
    @Test
    void findByTicker_shouldReturnInstrumentForGivenTicker() {
        String ticker = "AAPL";
        Instrument mockInstrument = new Instrument();
        mockInstrument.setTicker(ticker);
        mockInstrument.setAssetType("stock");

        when(instrumentMapper.findByTicker(ticker)).thenReturn(mockInstrument);

        Instrument result = instrumentRepository.findByTicker(ticker);

        assertNotNull(result);
        assertEquals(ticker, result.getTicker());
        assertEquals("stock", result.getAssetType());
        verify(instrumentMapper, times(1)).findByTicker(ticker);
    }

    /**
     * Tests finding all returns empty list
     */
    @Test
    void findAll_shouldReturnEmptyListWhenNoInstruments() {
        when(instrumentMapper.findAll()).thenReturn(new ArrayList<>());

        List<Instrument> result = instrumentRepository.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Tests finding by ticker returns null when not found
     */
    @Test
    void findByTicker_shouldReturnNullWhenInstrumentNotFound() {
        String ticker = "NONEXISTENT";

        when(instrumentMapper.findByTicker(ticker)).thenReturn(null);

        Instrument result = instrumentRepository.findByTicker(ticker);

        assertNull(result);
    }

    /**
     * Tests finding multiple instruments
     */
    @Test
    void findAll_shouldReturnMultipleInstruments() {
        List<Instrument> mockInstruments = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Instrument inst = new Instrument();
            inst.setTicker("TICK" + i);
            mockInstruments.add(inst);
        }

        when(instrumentMapper.findAll()).thenReturn(mockInstruments);

        List<Instrument> result = instrumentRepository.findAll();

        assertEquals(5, result.size());
    }

    /**
     * Tests finding by different tickers
     */
    @Test
    void findByTicker_shouldReturnDifferentInstrumentsForDifferentTickers() {
        Instrument inst1 = new Instrument();
        inst1.setTicker("AAPL");

        Instrument inst2 = new Instrument();
        inst2.setTicker("GOOGL");

        when(instrumentMapper.findByTicker("AAPL")).thenReturn(inst1);
        when(instrumentMapper.findByTicker("GOOGL")).thenReturn(inst2);

        Instrument result1 = instrumentRepository.findByTicker("AAPL");
        Instrument result2 = instrumentRepository.findByTicker("GOOGL");

        assertEquals("AAPL", result1.getTicker());
        assertEquals("GOOGL", result2.getTicker());
        assertNotEquals(result1.getTicker(), result2.getTicker());
    }

    /**
     * Tests finding by ticker with special characters
     */
    @Test
    void findByTicker_shouldHandleSpecialTickerFormats() {
        String ticker = "BTC/USD";
        Instrument mockInstrument = new Instrument();
        mockInstrument.setTicker(ticker);

        when(instrumentMapper.findByTicker(ticker)).thenReturn(mockInstrument);

        Instrument result = instrumentRepository.findByTicker(ticker);

        assertEquals(ticker, result.getTicker());
    }

    /**
     * Tests finding all with many instruments
     */
    @Test
    void findAll_shouldReturnLargeNumberOfInstruments() {
        List<Instrument> mockInstruments = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            Instrument inst = new Instrument();
            inst.setTicker("INST" + i);
            mockInstruments.add(inst);
        }

        when(instrumentMapper.findAll()).thenReturn(mockInstruments);

        List<Instrument> result = instrumentRepository.findAll();

        assertEquals(100, result.size());
    }

    /**
     * Tests mapper is called correct number of times
     */
    @Test
    void findByTicker_shouldCallMapperExactlyOnce() {
        String ticker = "AAPL";
        Instrument mockInstrument = new Instrument();

        when(instrumentMapper.findByTicker(ticker)).thenReturn(mockInstrument);

        instrumentRepository.findByTicker(ticker);

        verify(instrumentMapper, times(1)).findByTicker(ticker);
    }

    /**
     * Tests mapper is called for findAll
     */
    @Test
    void findAll_shouldCallMapperExactlyOnce() {
        when(instrumentMapper.findAll()).thenReturn(new ArrayList<>());

        instrumentRepository.findAll();

        verify(instrumentMapper, times(1)).findAll();
    }
}
