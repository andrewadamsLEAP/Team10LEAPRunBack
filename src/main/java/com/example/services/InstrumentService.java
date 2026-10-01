package com.example.services;

import org.springframework.stereotype.Service;

import com.example.entities.Instrument;
import com.example.repositories.InstrumentRepository;

import java.util.List;


@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

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
