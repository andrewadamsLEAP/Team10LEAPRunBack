package com.example.services;

import com.example.entities.Order;
import com.example.entities.Instrument;
import com.example.repositories.OrdersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduled service that automatically executes pending orders.
 * Runs every 5 seconds to check for pending orders and execute them at current market prices.
 * This simulates the market matching engine in a real trading application.
 * 
 * THIS WILL BE EXECUTED AUTOMATICALLY VIA KAFKA SCHEDULER SOON
 * 
 * Market Hours Behavior:
 * - STOCK orders: Only execute during market hours. Cancelled at market close.
 * - CRYPTO orders: Execute 24/7 (market hours don't apply).
 */
@Service
public class OrderExecutionScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderExecutionScheduler.class);
    
    private final OrdersRepository ordersRepository;
    private final OrdersService ordersService;
    private final MarketHoursService marketHoursService;
    private final InstrumentService instrumentService;
    
    public OrderExecutionScheduler(
            OrdersRepository ordersRepository,
            OrdersService ordersService,
            MarketHoursService marketHoursService,
            InstrumentService instrumentService) {
        this.ordersRepository = ordersRepository;
        this.ordersService = ordersService;
        this.marketHoursService = marketHoursService;
        this.instrumentService = instrumentService;
    }
    
    /**
     * Executes all pending orders at current market prices.
     * Runs every 5 seconds to check for pending orders and execute them at current market prices.
     * 
     * Market Hours Behavior:
     * - If market is CLOSED and order is for STOCK: Cancel the order
     * - If market is OPEN or order is for CRYPTO: Execute the order
     * 
     * For each pending order:
     * 1. Checks if market is open (for STOCK orders only)
     * 2. If market closed + STOCK: Cancels the order
     * 3. Otherwise: Fetches current market price (ask for BUY, bid for SELL) and executes
     * 4. Updates order status to FULFILLED or CANCELLED
     * 5. Updates client holdings (if executed)
     * 
     * If execution/cancellation fails for any order, logs the error and continues with next order.
     */

    //This is the line that will be changed to use Kafka scheduler in the future
    @Async
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void executePendingOrders() {
        logger.debug("Checking for pending orders to execute...");
        
        // Get all pending orders from repository
        List<Order> pendingOrders = ordersRepository.getPendingOrders();
        
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            logger.debug("No pending orders found");
            return;
        }
        
        logger.info("Found {} pending orders to process", pendingOrders.size());
        
        // Check if market is currently open
        boolean isMarketOpen = marketHoursService.isUsMarketHours();
        
        // Process each pending order
        for (Order order : pendingOrders) {
            try {
                // Check if this is a STOCK order and market is closed
                if (!isMarketOpen && isStockOrder(order.getTicker())) {
                    // Cancel STOCK orders when market is closed
                    logger.info("Market closed - Cancelling STOCK order: orderId={}, clientId={}, ticker={}, quantity={}", 
                        order.getOrderId(), 
                        order.getClientId(), 
                        order.getTicker(),
                        order.getQuantity());
                    
                    ordersService.cancelOrder(order.getOrderId());
                    logger.info("STOCK order cancelled at market close: orderId={}", order.getOrderId());
                } else {
                    // Execute the order (STOCK during open hours OR CRYPTO 24/7)
                    logger.info("Executing order: orderId={}, clientId={}, ticker={}, type={}, quantity={}", 
                        order.getOrderId(), 
                        order.getClientId(), 
                        order.getTicker(), 
                        order.getOrderType(),
                        order.getQuantity());
                    
                    // Call the existing executeOrder method from OrdersService
                    ordersService.executeOrder(order.getOrderId());
                    
                    logger.info("Order executed successfully: orderId={}", order.getOrderId());
                }
                
            } catch (IllegalArgumentException validationError) {
                // Execution-time validation failed (e.g., insufficient cash/holdings at current price)
                // Cancel the order instead of failing
                logger.warn("Order validation failed at execution - cancelling order: orderId={}, ticker={}, reason={}", 
                    order.getOrderId(), 
                    order.getTicker(),
                    validationError.getMessage());
                try {
                    ordersService.cancelOrder(order.getOrderId());
                    logger.info("Order cancelled due to validation failure: orderId={}", order.getOrderId());
                } catch (Exception cancelError) {
                    logger.error("Failed to cancel order after validation failure: orderId={}, error={}", 
                        order.getOrderId(), cancelError.getMessage());
                }
            } catch (Exception e) {
                logger.error("Failed to process order: orderId={}, ticker={}, error={}, cause={}", 
                    order.getOrderId(), 
                    order.getTicker(),
                    e.getMessage(), 
                    e.getCause() != null ? e.getCause().getMessage() : "unknown",
                    e);
                // Continue to next order instead of stopping
            }
        }
    }
    
    /**
     * Cancels all pending orders that are older than today's date.
     * This runs hourly to ensure stale orders from previous days are cleaned up.
     * Prevents orders from lingering indefinitely.
     */
    @Async
    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)  // Run hourly (every 60 minutes)
    public void cancelStaleOrders() {
        logger.debug("Checking for stale orders older than today...");
        
        LocalDate today = LocalDate.now();
        List<Order> pendingOrders = ordersRepository.getPendingOrders();
        
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            logger.debug("No pending orders found for stale order check");
            return;
        }
        
        int cancelledCount = 0;
        for (Order order : pendingOrders) {
            if (order.getOrderDate() != null) {
                // Convert OffsetDateTime to LocalDate for comparison
                LocalDate orderDate = order.getOrderDate().toLocalDate();
                
                // If order is from before today, cancel it
                if (orderDate.isBefore(today)) {
                    try {
                        logger.info("Cancelling stale order from previous day: orderId={}, clientId={}, ticker={}, orderDate={}", 
                            order.getOrderId(), 
                            order.getClientId(), 
                            order.getTicker(), 
                            orderDate);
                        
                        ordersService.cancelOrder(order.getOrderId());
                        cancelledCount++;
                        logger.info("Stale order cancelled: orderId={}", order.getOrderId());
                    } catch (Exception e) {
                        logger.error("Failed to cancel stale order: orderId={}, error={}", 
                            order.getOrderId(), e.getMessage());
                    }
                }
            }
        }
        
        if (cancelledCount > 0) {
            logger.info("Cancelled {} stale orders from previous days", cancelledCount);
        } else {
            logger.debug("No stale orders to cancel");
        }
    }
    
    /**
     * Determines if a ticker symbol is for a STOCK (as opposed to CRYPTO).
     * 
     * @param ticker the ticker symbol to check
     * @return true if this ticker is a STOCK, false if CRYPTO or other asset type
     */
    private boolean isStockOrder(String ticker) {
        try {
            Instrument instrument = instrumentService.getInstrumentByTicker(ticker);
            if (instrument != null) {
                return "STOCK".equalsIgnoreCase(instrument.getAssetType());
            }
        } catch (Exception e) {
            logger.debug("Could not determine asset type for ticker: {}", ticker, e);
        }
        // Default to true (assume STOCK) to be conservative with market hours
        return true;
    }
}
