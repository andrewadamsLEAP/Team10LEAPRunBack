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
 * CleanStaleOrdersService runs automatically in the background to clean up stale pending orders.
 * 
 * Responsibilities:
 * - Every hour: Clean up stale orders (orders from before today) and mark them as CANCELLED
 * 
 * Order Staleness Logic:
 * - Orders placed before today are considered "stale"
 * - Stale PENDING orders are automatically cancelled
 * - Prevents old orders from lingering indefinitely in PENDING state
 * 
 * Cleanup Flow:
 * 1. Query all PENDING orders from database
 * 2. Check each order's creation date
 * 3. If order is from before today, cancel it
 * 4. Log the number of orders cleaned up
 */
@Service
@Profile("!test")
public class CleanStaleOrdersService {
    private static final Logger logger = LoggerFactory.getLogger(CleanStaleOrdersService.class);

    private final OrdersRepository ordersRepository;
    private final OrdersService ordersService;

    public CleanStaleOrdersService(
            OrdersRepository ordersRepository,
            OrdersService ordersService) {
        this.ordersRepository = ordersRepository;
        this.ordersService = ordersService;
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
}