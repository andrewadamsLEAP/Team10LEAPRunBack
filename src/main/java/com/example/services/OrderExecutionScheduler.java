package com.example.services;

import com.example.entities.Order;
import com.example.repositories.OrdersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/**
 * OrderExecutionScheduler runs automatically in the background to execute pending orders.
 * 
 * Responsibilities:
 * 1. Every 5 seconds: Poll database for PENDING orders and execute them with current market prices
 * 2. Every hour: Clean up stale orders (orders from before today) and mark them as CANCELLED
 * 
 * Market Hours Logic:
 * - STOCK orders: Only execute during market hours (9:30 AM - 4:00 PM ET, Mon-Fri)
 * - CRYPTO orders: Execute 24/7
 * 
 * Price Validation:
 * - Fetch current market price at execution time (not placement time)
 * - For BUY orders: Use current ask price (what sellers are asking)
 * - For SELL orders: Use current bid price (what buyers are bidding)
 * - Re-validate client has sufficient cash/holdings at current price
 * - If validation fails: Cancel order instead of error
 * 
 * Execution Flow:
 * 1. Check if order is still PENDING
 * 2. If STOCK: Check if market is open; cancel if closed
 * 3. Fetch current market price
 * 4. Update order.price to current price
 * 5. Re-validate cash/holdings at current price
 * 6. If valid: Execute order, update holdings, update cash, mark FULFILLED
 * 7. If invalid: Cancel order
 */
@Service
@Profile("!test")
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
     * Execute all pending orders every 5 seconds
     * Runs asynchronously to prevent blocking market data refresh
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Async
    public void executePendingOrders() {
        logger.debug("Starting execution of pending orders");

        try {
            List<Order> pendingOrders = ordersRepository.getPendingOrders();

            if (pendingOrders == null || pendingOrders.isEmpty()) {
                logger.debug("No pending orders found");
                return;
            }

            logger.info("Found {} pending orders to process", pendingOrders.size());

            for (Order order : pendingOrders) {
                try {
                    // Check if STOCK and market is closed - cancel if so
                    if (isStockOrder(order.getTicker()) && !marketHoursService.isUsMarketHours()) {
                        logger.warn("Stock order {} for {} cannot be executed - market closed. Cancelling.", 
                            order.getOrderId(), order.getTicker());
                        ordersService.cancelOrder(order.getOrderId());
                        continue;
                    }

                    // Execute order with current market price
                    logger.info("Executing order {}: {} {} shares of {} at market price", 
                        order.getOrderId(), order.getOrderType(), order.getQuantity(), order.getTicker());
                    ordersService.executeOrder(order.getOrderId());

                } catch (IllegalArgumentException e) {
                    // Validation failure - cancel the order
                    logger.warn("Order {} validation failed: {}. Cancelling order.", order.getOrderId(), e.getMessage());
                    try {
                        ordersService.cancelOrder(order.getOrderId());
                    } catch (Exception cancelEx) {
                        logger.error("Failed to cancel order {} after validation failure", order.getOrderId(), cancelEx);
                    }
                } catch (Exception e) {
                    logger.error("Error executing order {}", order.getOrderId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Error in pending order execution scheduler", e);
        }
    }

    /**
     * Clean up stale orders every hour
     * Cancels all PENDING orders from before today
     * Prevents old orders from lingering in PENDING state
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)  // 1 hour delay, 2 minute initial delay
    @Async
    @Transactional
    public void cancelStaleOrders() {
        logger.debug("Starting cleanup of stale pending orders");

        try {
            List<Order> allPendingOrders = ordersRepository.getPendingOrders();

            if (allPendingOrders == null || allPendingOrders.isEmpty()) {
                logger.debug("No pending orders to check for staleness");
                return;
            }

            LocalDate today = LocalDate.now();
            int staleCount = 0;

            for (Order order : allPendingOrders) {
                // Extract date from order_date (OffsetDateTime)
                LocalDate orderDate = order.getOrderDate().toLocalDate();

                // If order is from before today, cancel it
                if (orderDate.isBefore(today)) {
                    logger.info("Cancelling stale order {} placed on {}", order.getOrderId(), orderDate);
                    try {
                        ordersService.cancelOrder(order.getOrderId());
                        staleCount++;
                    } catch (Exception e) {
                        logger.error("Error cancelling stale order {}", order.getOrderId(), e);
                    }
                }
            }

            if (staleCount > 0) {
                logger.info("Stale order cleanup complete. Cancelled {} orders", staleCount);
            }

        } catch (Exception e) {
            logger.error("Error in stale order cleanup scheduler", e);
        }
    }

    /**
     * Determine if a ticker is a STOCK (vs CRYPTO)
     * STOCK orders have market hours restrictions
     * CRYPTO orders trade 24/7
     * 
     * @param ticker the ticker symbol
     * @return true if this is a stock, false if crypto
     */
    private boolean isStockOrder(String ticker) {
        try {
            var instrument = instrumentService.getInstrumentByTicker(ticker);
            return "STOCK".equalsIgnoreCase(instrument.getAssetType());
        } catch (Exception e) {
            logger.warn("Could not determine asset type for ticker {}, assuming STOCK", ticker);
            return true;  // Default to STOCK if unknown
        }
    }
}
