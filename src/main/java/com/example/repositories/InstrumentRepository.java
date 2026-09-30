package com.example.instruments;

import org.springframework.stereotype.Repository;
import java.util.List;

@Repository 
public class InstrumentRepository {
    private final InstrumentMapper instrumentMapper;

    public InstrumentRepository(InstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
    }

    public List<Instrument> findAll() {
        return instrumentMapper.findAll();
    }

    public Instrument findByTicker(String ticker) {
        return instrumentMapper.findByTicker(ticker);
    }
}
