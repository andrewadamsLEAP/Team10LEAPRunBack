package com.example.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.services.InstrumentService;
import com.example.entities.Instrument;

import com.example.entities.Instrument;
import com.example.services.InstrumentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentController {
    private final InstrumentService instrumentService;

    /**
     * Constructs a new InstrumentController with the specified InstrumentService.
     *
     * @param instrumentService the InstrumentService instance to use
     */
    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    /**
     * Retrieves all instruments.
     *
     * @return a list of all instruments
     */
    @GetMapping
    public List<Instrument> getAllInstruments() {
        return instrumentService.getAllInstruments();
    }

    /**
     * Retrieves an instrument by its ticker symbol.
     *
     * @param ticker the ticker symbol of the instrument to retrieve
     * @return the instrument with the specified ticker
     */
    @GetMapping("/{ticker}")
    public Instrument getInstrumentByTicker(@PathVariable String ticker) {
        return instrumentService.getInstrumentByTicker(ticker);
    }
}
