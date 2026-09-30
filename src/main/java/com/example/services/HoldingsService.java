package com.example.services;

import com.example.entities.Order;
import com.example.entities.Holding;
import org.springframework.stereotype.Service;
import com.example.repositories.HoldingsRepository; 

import com.example.exceptions.Validate;
import com.example.exceptions.InvalidArgumentsException;

@Service
public class HoldingsService {
    
    private final HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }


    //TEST METHOD
    public String test() {
        return "Test service works!";
    }

    /**
     * Get holding for a specific client and ticker
     */
    public Holding getHolding(Long clientId, String ticker) {
        Validate.validateClientId(clientId);
        Validate.validateTicker(ticker);
        return holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker);
    }


    
    /**
     * Get all holdings for a specific client
     */
    public java.util.List<Holding> getClientHoldings(Long clientId) {
        Validate.validateClientId(clientId);
        return holdingsRepository.getHoldingsByClient(clientId);
    }


    
    /**
     * Get quantity of a specific ticker for a client
     */
    public Integer getQuantity(Long clientId, String ticker) {
        Validate.validateClientId(clientId);
        Validate.validateTicker(ticker);
        return holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
    }



    /**
     * Buy stock logic - increase quantity for client/ticker
     * If client doesn't own this ticker yet, create new holding
     */
    public void buyStock(Long clientId, String ticker, Integer quantity) {
        Validate.validateClientId(clientId);
        Validate.validateTicker(ticker);
        Validate.validateQuantity(quantity);
        Integer currentQty = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        
        if (currentQty == null || currentQty == 0) {
            // First time buying this ticker
            Holding holding = new Holding();
            holding.setClient_Id(clientId);
            holding.setTicker(ticker);
            holding.setQuantity(quantity);
            holdingsRepository.createHolding(holding);
        } else {
            // Already owns this ticker, add quantity
            holdingsRepository.updateBuyHolding(quantity, clientId, ticker);
        }
    }



    /**
     * Sell stock logic - decrease quantity for client/ticker
     * Validates client has sufficient shares
     */
    public void sellStock(Long clientId, String ticker, Integer quantity) {
        Validate.validateClientId(clientId);
        Validate.validateTicker(ticker);
        Validate.validateQuantity(quantity);
        Integer currentQty = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        
        if (currentQty == null || currentQty < quantity) {
            String msg = "Insufficient shares to sell. Current: " + 
                         (currentQty == null ? 0 : currentQty) + 
                         ", Trying to sell: " + quantity;
            throw new InvalidArgumentsException("Invalid Sell Operation", msg);
        }
        
        holdingsRepository.updateSellHolding(quantity, clientId, ticker);
    }



    /**
     * Update holdings based on fulfilled order
     * Called by OrdersService when order status is set to FULFILLED
     * If BUY: increases quantity
     * If SELL: decreases quantity
     */
    public void updateHoldingsForOrder(Order order) {
        if (order.orderType() == Order.OrderType.BUY) {
            buyStock(order.clientId(), order.ticker(), order.quantity());
        } else if (order.orderType() == Order.OrderType.SELL) {
            sellStock(order.clientId(), order.ticker(), order.quantity());
        }
    }
}
