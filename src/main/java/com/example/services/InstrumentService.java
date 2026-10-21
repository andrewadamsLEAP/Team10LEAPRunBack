package com.example.services;

import org.springframework.stereotype.Service;
import com.example.repositories.InstrumentRepository;
import com.example.entities.Instrument;

import com.example.entities.Instrument;
import com.example.repositories.InstrumentRepository;

import java.util.List;

@Service
public class InstrumentService {
    
    /**
     * The InstrumentRepository instance used to interact with the database.
     */
    private final InstrumentRepository instrumentRepository;

    /**
     * Constructs a new InstrumentService with the specified InstrumentRepository.
     *
     * @param instrumentRepository the InstrumentRepository instance to use
     */
    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    /**
     * Retrieves a list of all instruments from the database.
     *
     * @return a list of all instruments
     */
    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findAll()
            .stream()
            .peek(this::applyDisplayNameFallback)
            .toList();
    }


    /**
     * Main method to get instrument information by the ticker symbol.
     * @param ticker the ticker symbol of the instrument to retrieve
     * @return the Instrument object corresponding to the given ticker symbol
     */
    public Instrument getInstrumentByTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }

        String normalizedTicker = ticker.toUpperCase().trim();
        Instrument instrument = instrumentRepository.findByTicker(normalizedTicker);
        
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument not found: " + normalizedTicker);
        }
        
        return applyDisplayNameFallback(instrument);
    }

    /**
     * Main method to check if an instrument exists by its ticker.
     * @param ticker the ticker symbol of the instrument to check
     * @return true if the instrument exists, false otherwise
     */
    public boolean instrumentExists(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return false;
        }

        String normalizedTicker = ticker.toUpperCase().trim();
        return instrumentRepository.findByTicker(normalizedTicker) != null;
    }

    /**
     * Updates daily market statistics without changing the instrument ticker or type.
     */
    public int updateMarketStats(
            String ticker,
            Double previousClose,
            Double open,
            Integer volume,
            Double avgVolume) {
        return instrumentRepository.updateMarketStats(
                ticker, previousClose, open, volume, avgVolume);
    }

    private Instrument applyDisplayNameFallback(Instrument instrument) {
        if (instrument != null
                && (instrument.getDisplayName() == null
                || instrument.getDisplayName().isBlank())) {
            instrument.setDisplayName(instrument.getTicker());
        }
        return instrument;
    }
}
