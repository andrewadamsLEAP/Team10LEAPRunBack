package com.example.services;

import com.example.entities.Order;
import com.example.entities.Holding;
import com.example.DTOs.holdings.HoldingResponse;
import com.example.DTOs.holdings.QuantityResponse;
import com.example.DTOs.holdings.BuyStockResponse;
import com.example.DTOs.holdings.SellStockResponse;
import com.example.DTOs.holdings.HoldingDtoConverter;
import com.example.exceptions.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.repositories.HoldingsRepository;
import com.example.repositories.ClientsRepository;

import java.util.List;

@Service
public class HoldingsService {
    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);
    
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
        return "Test service works! Hooray!";
    }

    /**
     * Get holding for a specific client and ticker
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @return the holding response for the specified client and ticker
     * @throws IllegalArgumentException if clientId or ticker is invalid
     */
    public HoldingResponse getHolding(Long clientId, String ticker) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        Validate.validateTicker(ticker);
        Holding holding = holdingsRepository.getHoldingsByClientAndTicker(clientId, ticker);
        return new HoldingResponse(holding.getClient_Id(), holding.getTicker(), holding.getQuantity());
    }


    
    /**
     * Get all holdings for a specific client
     * @param clientId the ID of the client
     * @return a list of holding responses for the specified client
     * @throws IllegalArgumentException if clientId is invalid
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
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @return the quantity response for the specified client and ticker
     * @throws IllegalArgumentException if clientId or ticker is invalid
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
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the quantity to buy
     * @return the buy stock response for the specified client, ticker, and quantity
     * @throws IllegalArgumentException if clientId, ticker, or quantity is invalid
     */     
    public BuyStockResponse buyStock(Long clientId, String ticker, Integer quantity) {
        logger.info("Buy stock request: clientId={}, ticker={}, quantity={}", clientId, ticker, quantity);
        
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
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the quantity to sell
     * @return the sell stock response for the specified client, ticker, and quantity
     * @throws IllegalArgumentException if clientId, ticker, or quantity is invalid
     */
    public SellStockResponse sellStock(Long clientId, String ticker, Integer quantity) {
        logger.info("Sell stock request: clientId={}, ticker={}, quantity={}", clientId, ticker, quantity);
        
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        Validate.validateTicker(ticker);
        Validate.validateQuantity(quantity);
        Integer currentQty = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        
        if (currentQty == null || currentQty < quantity) {
            logger.warn("Insufficient shares to sell: clientId={}, ticker={}, requested={}, available={}", clientId, ticker, quantity, (currentQty == null ? 0 : currentQty));
            throw new IllegalArgumentException(
                    "Insufficient shares to sell. Current: " + 
                    (currentQty == null ? 0 : currentQty) + 
                    ", Trying to sell: " + quantity
            );
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
     * @param order the fulfilled order containing clientId, ticker, quantity, and order type
     * @throws IllegalArgumentException if the order is invalid or contains invalid data
     */
    public void updateHoldingsForOrder(Order order) {
        if (order.getOrderType() == Order.OrderType.BUY) {
            buyStock(order.getClientId(), order.getTicker(), order.getQuantity());
        } else if (order.getOrderType() == Order.OrderType.SELL) {
            sellStock(order.getClientId(), order.getTicker(), order.getQuantity());
        }
    }
}
