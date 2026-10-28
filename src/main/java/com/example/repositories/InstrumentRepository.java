package com.example.repositories;

import org.springframework.stereotype.Repository;  
import com.example.mappers.InstrumentMapper;
import com.example.entities.Instrument;
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

    /**
     * Persists the latest daily statistics for an existing instrument.
     */
    public int updateMarketStats(
            String ticker,
            Double previousClose,
            Double open,
            Integer volume,
            Double avgVolume) {
        return instrumentMapper.updateMarketStats(
                ticker, previousClose, open, volume, avgVolume);
    }

    public int updatePreviousClose(String ticker, Double previousClose) {
        return instrumentMapper.updatePreviousClose(ticker, previousClose);
    }
}
