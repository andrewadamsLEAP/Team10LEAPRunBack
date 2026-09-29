package com.example.holdings;

import org.springframework.stereotype.Repository;

@Repository
public class HoldingsRepository {
    private final HoldingsMapper holdingsMapper;

    public HoldingsRepository(HoldingsMapper holdingsMapper) {
        this.holdingsMapper = holdingsMapper;
    }


    public Holding getHoldingsByClientAndTicker(Long clientId, String ticker) {
        return holdingsMapper.getHoldingsByClientAndTicker(clientId, ticker);
    }


    public Integer getQuantityByClientAndTicker(Long clientId, String ticker) {
        return holdingsMapper.getQuantityByClientAndTicker(clientId, ticker);
    }

    public Holding getHoldingsByClient(Long clientId) {
        return holdingsMapper.getHoldingsByClient(clientId);
    }


    public Holding createHolding(Holding holding) {
        holdingsMapper.createHolding(holding);
        return holding;
    }


    public int updateBuyHolding(Integer quantity, Long client_id, String ticker) {
        return holdingsMapper.updateBuyHolding(quantity, client_id, ticker);
    }

    public int updateSellHolding(Integer quantity, Long client_id, String ticker) {
        return holdingsMapper.updateSellHolding(quantity, client_id, ticker);
    }
    
}
