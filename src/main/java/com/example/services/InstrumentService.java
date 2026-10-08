package com.example.services;

import org.springframework.stereotype.Service;
import com.example.repositories.InstrumentRepository;
import com.example.entities.Instrument;

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
        return instrumentRepository.findAll();
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
        
        return instrument;
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
}
