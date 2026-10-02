package com.example.services;

import org.springframework.stereotype.Service;
import com.example.repositories.InstrumentRepository;
import com.example.entities.Instrument;

import com.example.entities.Instrument;
import com.example.repositories.InstrumentRepository;

import java.util.List;

//
// Hey Kevin I set up some comments for you to kinda copy for the other functions below
// Just go through every Instruments method and add similar Javadoc comments
//


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

    public boolean instrumentExists(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return false;
        }

        String normalizedTicker = ticker.toUpperCase().trim();
        return instrumentRepository.findByTicker(normalizedTicker) != null;
    }
}
