package com.example.instruments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstrumentController.
 * Tests the controller layer logic with mocked service.
 */
@ExtendWith(MockitoExtension.class)
class InstrumentControllerTest {

    @Mock
    private InstrumentService instrumentService;

    private InstrumentController instrumentController;

    @BeforeEach
    void setUp() {
        instrumentController = new InstrumentController(instrumentService);
    }

    @Test
    void testGetAllInstruments_CallsServiceAndReturnsResult() {
        // Arrange
        List<Instrument> expectedInstruments = Arrays.asList(
                createInstrument("AAPL", 150.0, 149.0, "STOCK"),
                createInstrument("GOOGL", 140.0, 139.0, "STOCK")
        );
        when(instrumentService.getAllInstruments()).thenReturn(expectedInstruments);

        // Act
        List<Instrument> result = instrumentController.getAllInstruments();

        // Assert
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        assertEquals("GOOGL", result.get(1).getTicker());
        verify(instrumentService, times(1)).getAllInstruments();
    }

    @Test
    void testGetAllInstruments_ReturnsEmptyList() {
        // Arrange
        when(instrumentService.getAllInstruments()).thenReturn(Arrays.asList());

        // Act
        List<Instrument> result = instrumentController.getAllInstruments();

        // Assert
        assertTrue(result.isEmpty());
        verify(instrumentService, times(1)).getAllInstruments();
    }

    @Test
    void testGetInstrumentByTicker_CallsServiceAndReturnsResult() {
        // Arrange
        Instrument aapl = createInstrument("AAPL", 150.0, 149.0, "STOCK");
        when(instrumentService.getInstrumentByTicker("AAPL")).thenReturn(aapl);

        // Act
        Instrument result = instrumentController.getInstrumentByTicker("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        assertEquals(150.0, result.getOpen());
        verify(instrumentService, times(1)).getInstrumentByTicker("AAPL");
    }

    @Test
    void testGetInstrumentByTicker_ThrowsExceptionWhenNotFound() {
        // Arrange
        when(instrumentService.getInstrumentByTicker("INVALID"))
                .thenThrow(new IllegalArgumentException("Instrument not found: INVALID"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentController.getInstrumentByTicker("INVALID")
        );

        assertEquals("Instrument not found: INVALID", exception.getMessage());
        verify(instrumentService, times(1)).getInstrumentByTicker("INVALID");
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
