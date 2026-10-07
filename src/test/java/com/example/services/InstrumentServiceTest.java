package com.example.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.repositories.InstrumentRepository;
import com.example.services.InstrumentService;
import com.example.entities.Instrument;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    private InstrumentService instrumentService;

    @BeforeEach
    void setUp() {
        instrumentService = new InstrumentService(instrumentRepository);
    }

    @Test
    void testGetAllInstruments_ReturnsAllInstruments() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        Instrument googl = createInstrument("GOOGL", 140.0, 139.0, "STOCK");
        List<Instrument> expectedInstruments = Arrays.asList(aapl, googl);

        when(instrumentRepository.findAll()).thenReturn(expectedInstruments);

        // Act
        List<Instrument> result = instrumentService.getAllInstruments();

        // Assert
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("GOOGL", result.get(1).getTicker());
        verify(instrumentRepository, times(1)).findAll();
    }

    @Test
    void testGetAllInstruments_ReturnsEmptyList() {
        // Arrange
        when(instrumentRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<Instrument> result = instrumentService.getAllInstruments();

        // Assert
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(instrumentRepository, times(1)).findAll();
    }

    @Test
    void testGetInstrumentByTicker_Success() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(aapl);

        // Act
        Instrument result = instrumentService.getInstrumentByTicker("aapl");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        verify(instrumentRepository, times(1)).findByTicker("AAPL");
    }

    @Test
    void testGetInstrumentByTicker_NormalizesToUppercase() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(aapl);

        // Act
        Instrument result = instrumentService.getInstrumentByTicker("AaPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        verify(instrumentRepository, times(1)).findByTicker("AAPL");
    }

    @Test
    void testGetInstrumentByTicker_TrimsWhitespace() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(aapl);

        // Act
        Instrument result = instrumentService.getInstrumentByTicker("  aapl  ");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        verify(instrumentRepository, times(1)).findByTicker("AAPL");
    }

    @Test
    void testGetInstrumentByTicker_ThrowsExceptionWhenNotFound() {
        // Arrange
        when(instrumentRepository.findByTicker("INVALID")).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentService.getInstrumentByTicker("INVALID")
        );

        assertEquals("Instrument not found: INVALID", exception.getMessage());
        verify(instrumentRepository, times(1)).findByTicker("INVALID");
    }

    @Test
    void testGetInstrumentByTicker_ThrowsExceptionForNullTicker() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentService.getInstrumentByTicker(null)
        );

        assertEquals("Ticker cannot be null or empty", exception.getMessage());
        verify(instrumentRepository, never()).findByTicker(any());
    }

    @Test
    void testGetInstrumentByTicker_ThrowsExceptionForEmptyTicker() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentService.getInstrumentByTicker("")
        );

        assertEquals("Ticker cannot be null or empty", exception.getMessage());
        verify(instrumentRepository, never()).findByTicker(any());
    }

    @Test
    void testGetInstrumentByTicker_ThrowsExceptionForWhitespaceTicker() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentService.getInstrumentByTicker("   ")
        );

        assertEquals("Ticker cannot be null or empty", exception.getMessage());
        verify(instrumentRepository, never()).findByTicker(any());
    }

    @Test
    void testInstrumentExists_ReturnsTrueWhenFound() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(aapl);

        // Act
        boolean result = instrumentService.instrumentExists("aapl");

        // Assert
        assertTrue(result);
        verify(instrumentRepository, times(1)).findByTicker("AAPL");
    }

    @Test
    void testInstrumentExists_ReturnsFalseWhenNotFound() {
        // Arrange
        when(instrumentRepository.findByTicker("INVALID")).thenReturn(null);

        // Act
        boolean result = instrumentService.instrumentExists("INVALID");

        // Assert
        assertFalse(result);
        verify(instrumentRepository, times(1)).findByTicker("INVALID");
    }

    @Test
    void testInstrumentExists_ReturnsFalseForNullTicker() {
        // Act
        boolean result = instrumentService.instrumentExists(null);

        // Assert
        assertFalse(result);
        verify(instrumentRepository, never()).findByTicker(any());
    }

    @Test
    void testInstrumentExists_ReturnsFalseForEmptyTicker() {
        // Act
        boolean result = instrumentService.instrumentExists("");

        // Assert
        assertFalse(result);
        verify(instrumentRepository, never()).findByTicker(any());
    }

    private Instrument createInstrument(String ticker, Double open, Double previousClose, String assetType) {
        Instrument instrument = new Instrument();
        instrument.setTicker(ticker);
        instrument.setOpen(open);
        instrument.setPreviousClose(previousClose);
        instrument.setAssetType(assetType);
        instrument.setVolume(1000);
        instrument.setAvgVolume(1500.0);
        return instrument;
    }
}
