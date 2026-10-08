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
 * 
 * Note: Stale order cleanup is handled by CleanStaleOrdersService (runs every hour)
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
     * Execute pending orders every 5 seconds
     * Checks market hours for STOCK orders, executes if valid
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    @Async
    @Transactional
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
