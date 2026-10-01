package com.example.services;

import com.example.entities.Order;
import com.example.entities.Holding;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.DTOs.holdings.BuyStockResponse;
import com.example.DTOs.holdings.SellStockResponse;
import com.example.DTOs.holdings.HoldingDtoConverter;
import org.springframework.stereotype.Service;
import com.example.repositories.HoldingsRepository;
import com.example.repositories.ClientsRepository;

import com.example.exceptions.Validate;
import com.example.exceptions.InvalidArgumentsException;

@Service
public class HoldingsService {
    
    private final HoldingsRepository holdingsRepository;
    private final ClientsRepository clientsRepository;
    private final HoldingDtoConverter holdingDtoConverter;

    public HoldingsService(HoldingsRepository holdingsRepository, ClientsRepository clientsRepository, HoldingDtoConverter holdingDtoConverter) {
        this.holdingsRepository = holdingsRepository;
        this.clientsRepository = clientsRepository;
        this.holdingDtoConverter = holdingDtoConverter;
    }


    //TEST METHOD
    public String test() {
        return "Test service works!";
    }

    /**
     * Get holding for a specific client and ticker
     */
    public HoldingResponse getHolding(Long clientId, String ticker) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        Validate.validateTicker(ticker);
        Holding holding = holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker);
        return new HoldingResponse(holding.getClient_Id(), holding.getTicker(), holding.getQuantity());
    }


    
    /**
     * Get all holdings for a specific client
     */
    public java.util.List<HoldingResponse> getAllClientHoldings(Long clientId) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        return holdingsRepository.getHoldingsByClient(clientId)
            .stream()
            .map(h -> new HoldingResponse(h.getClient_Id(), h.getTicker(), h.getQuantity()))
            .toList();
    }


    
    /**
     * Get quantity of a specific ticker for a client
     */
    public QuantityResponse getQuantity(Long clientId, String ticker) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        Validate.validateTicker(ticker);
        Integer quantity = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        return holdingDtoConverter.toQuantityResponse(quantity);
    }



    /**
     * Buy stock logic - increase quantity for client/ticker
     * If client doesn't own this ticker yet, create new holding
     */
    public BuyStockResponse buyStock(Long clientId, String ticker, Integer quantity) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
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
            return holdingDtoConverter.toBuyStockResponse(clientId, ticker, quantity);
        } else {
            // Already owns this ticker, add quantity
            holdingsRepository.updateBuyHolding(quantity, clientId, ticker);
            Integer newQty = currentQty + quantity;
            return holdingDtoConverter.toBuyStockResponse(clientId, ticker, newQty);
        }
    }



    /**
     * Sell stock logic - decrease quantity for client/ticker
     * Validates client has sufficient shares
     */
    public SellStockResponse sellStock(Long clientId, String ticker, Integer quantity) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
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
        Integer newQty = currentQty - quantity;
        return holdingDtoConverter.toSellStockResponse(clientId, ticker, newQty);
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
