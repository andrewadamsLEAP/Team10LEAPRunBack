package com.example.instruments;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api//v1/instruments")
public class InstrumentController {
    private final InstrumentService instrumentService;


    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<Instrument> getAllInstruments() {
        return instrumentService.getAllInstruments();
    }

    @GetMapping("/{ticker}")
    public Instrument getInstrumentByTicker(@PathVariable String ticker) {
        return instrumentService.getInstrumentByTicker(ticker);
    }
}
