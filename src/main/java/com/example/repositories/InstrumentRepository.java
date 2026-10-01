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
}
