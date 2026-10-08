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

import java.math.BigDecimal;

@Service
public class HoldingsService {
    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);
    
    private final HoldingsRepository holdingsRepository;
    private final ClientsRepository clientsRepository;
    private final HoldingDtoConverter holdingDtoConverter;
    private final ClientsService clientsService;

    public HoldingsService(HoldingsRepository holdingsRepository, ClientsRepository clientsRepository, HoldingDtoConverter holdingDtoConverter, ClientsService clientsService) {
        this.holdingsRepository = holdingsRepository;
        this.clientsRepository = clientsRepository;
        this.holdingDtoConverter = holdingDtoConverter;
        this.clientsService = clientsService;
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
     * Validate common holdings transaction parameters.
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the quantity
     */
    private void validateHoldingsTransaction(Long clientId, String ticker, Integer quantity) {
        Validate.validateClientId(clientId, () -> clientsRepository.findClientById(clientId) != null);
        Validate.validateTicker(ticker);
        Validate.validateQuantity(quantity);
    }

    /**
     * Update holdings for a buy or sell operation.
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the quantity to transact
     * @param isBuy true for buy, false for sell
     * @return the new quantity after the operation
     */
    private Integer updateHoldings(Long clientId, String ticker, Integer quantity, boolean isBuy) {
        Integer currentQty = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        Integer newQty;

        if (isBuy) {
            newQty = currentQty == null ? quantity : currentQty + quantity;
            if (currentQty == null || currentQty == 0) {
                Holding holding = new Holding();
                holding.setClient_Id(clientId);
                holding.setTicker(ticker);
                holding.setQuantity(quantity);
                holdingsRepository.createHolding(holding);
            } else {
                holdingsRepository.updateBuyHolding(quantity, clientId, ticker);
            }
        } else {
            newQty = currentQty - quantity;
            holdingsRepository.updateSellHolding(quantity, clientId, ticker);
        }

        return newQty;
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
        
        validateHoldingsTransaction(clientId, ticker, quantity);
        Integer newQty = updateHoldings(clientId, ticker, quantity, true);
        
        return holdingDtoConverter.toBuyStockResponse(clientId, ticker, newQty);
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
        
        validateHoldingsTransaction(clientId, ticker, quantity);
        Integer currentQty = holdingsRepository.getQuantityByClientAndTicker(clientId, ticker);
        
        if (currentQty == null || currentQty < quantity) {
            logger.warn("Insufficient shares to sell: clientId={}, ticker={}, requested={}, available={}", clientId, ticker, quantity, (currentQty == null ? 0 : currentQty));
            throw new IllegalArgumentException(
                    "Insufficient shares to sell. Current: " + 
                    (currentQty == null ? 0 : currentQty) + 
                    ", Trying to sell: " + quantity
            );
        }
        
        Integer newQty = updateHoldings(clientId, ticker, quantity, false);
        return holdingDtoConverter.toSellStockResponse(clientId, ticker, newQty);
    }



    /**
     * Update holdings based on fulfilled order
     * Called by OrdersService when order status is set to FULFILLED
     * Updates both holdings (shares/crypto) and client cash amount:
     * 
     * BUY order: Increases quantity, deducts cash (quantity * price)
     * SELL order: Decreases quantity, adds cash (quantity * price)
     * 
     * @param order the fulfilled order containing clientId, ticker, quantity, order type, and execution price
     * @throws IllegalArgumentException if the order is invalid or contains invalid data
     */
    public void updateHoldingsForOrder(Order order) {
        BigDecimal totalCost = new BigDecimal(order.getQuantity()).multiply(order.getPrice());
        
        if (order.getOrderType() == Order.OrderType.BUY) {
            buyStock(order.getClientId(), order.getTicker(), order.getQuantity());
            // Deduct cash for BUY order (negative change)
            clientsService.updateCashAmount(order.getClientId(), totalCost.negate());
            logger.info("BUY order executed: clientId={}, ticker={}, quantity={}, price={}, totalCost={}", 
                        order.getClientId(), order.getTicker(), order.getQuantity(), order.getPrice(), totalCost);
        } else if (order.getOrderType() == Order.OrderType.SELL) {
            sellStock(order.getClientId(), order.getTicker(), order.getQuantity());
            // Add cash for SELL order (positive change)
            clientsService.updateCashAmount(order.getClientId(), totalCost);
            logger.info("SELL order executed: clientId={}, ticker={}, quantity={}, price={}, totalProceeds={}", 
                        order.getClientId(), order.getTicker(), order.getQuantity(), order.getPrice(), totalCost);
        }
    }
}
