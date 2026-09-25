package com.example.orders;
import com.example.generalServices.MarketHoursService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final MarketHoursService marketHoursService;

    public OrdersService(
            OrdersRepository ordersRepository,
            MarketHoursService marketHoursService) {

        this.ordersRepository = ordersRepository;
        this.marketHoursService = marketHoursService;
    }

    // =========================================================
    //                      SEARCH FUNCTIONS
    // =========================================================

    public Order getOrderById(Long orderId) {

        validateId(orderId, "Order ID");

        Order order = ordersRepository.getOrderById(orderId);

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order not found: " + orderId
            );
        }

        return order;
    }


    public List<Order> getFulfilledOrders(Long clientId) {

        validateId(clientId, "Client ID");

        return ordersRepository.getFulfilledOrders(clientId);
    }


    public List<Order> getCancelledOrders() {

        return ordersRepository.getCancelledOrders();
    }


    public List<Order> getPendingSellOrdersForTicker(
            String ticker) {

        validateTicker(ticker);

        return ordersRepository.getPendingSellOrdersForTicker(
                ticker.toUpperCase()
        );
    }


    public List<Order> getPendingBuyOrdersForTicker(
            String ticker) {

        validateTicker(ticker);

        return ordersRepository.getPendingBuyOrdersForTicker(
                ticker.toUpperCase()
        );
    }


    // =========================================================
    //                  PLACE BUY ORDER
    // =========================================================

    @Transactional
    public Order placeBuyOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        validateOrder(
                clientId,
                ticker,
                quantity,
                price
        );

        Order order = new Order(
                null,
                clientId,
                ticker.toUpperCase(),
                Order.OrderType.BUY,
                Order.OrderStatus.PENDING,
                quantity,
                price,
                OffsetDateTime.now()
        );

        return ordersRepository.createOrder(order);
    }


    // =========================================================
    //                  PLACE SELL ORDER
    // =========================================================

    @Transactional
    public Order placeSellOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        validateOrder(
                clientId,
                ticker,
                quantity,
                price
        );

        Order order = new Order(
                null,
                clientId,
                ticker.toUpperCase(),
                Order.OrderType.SELL,
                Order.OrderStatus.PENDING,
                quantity,
                price,
                OffsetDateTime.now()
        );

        return ordersRepository.createOrder(order);
    }


    // =========================================================
    //                       CANCEL ORDER
    // =========================================================

    @Transactional
    public Order cancelOrder(Long orderId) {

        Order order = getOrderById(orderId);

        if (order.orderStatus() != Order.OrderStatus.PENDING) {

            throw new IllegalStateException(
                    "Order " + orderId +
                    " cannot be cancelled because it is " +
                    order.orderStatus()
            );
        }

        int updated = ordersRepository.updateOrderStatus(
                orderId,
                Order.OrderStatus.CANCELLED
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Order " + orderId +
                    " is no longer pending and could not be cancelled."
            );
        }

        return getOrderById(orderId);
    }


    // =========================================================
    //                    EXECUTE ORDER
    // =========================================================

    @Transactional
    public Order executeOrder(Long orderId) {

        Order order = getOrderById(orderId);

        if (order.orderStatus() != Order.OrderStatus.PENDING) {

            throw new IllegalStateException(
                    "Only pending orders can be executed."
            );
        }

        int updated = ordersRepository.updateOrderStatus(
                orderId,
                Order.OrderStatus.FULFILLED
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Order " + orderId +
                    " is no longer pending and could not be executed."
            );
        }

        return getOrderById(orderId);
    }


    // =========================================================
    //                       VALIDATION
    // =========================================================

    private void validateOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        // Orders may only be placed while the US market is open.
        if (!marketHoursService.isUsMarketHours()) {

            throw new IllegalStateException(
                    "Orders can only be placed during US market hours."
            );
        }

        validateId(clientId, "Client ID");

        validateTicker(ticker);

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        if (price == null ||
                price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Price must be greater than zero."
            );
        }
    }


    private void validateTicker(String ticker) {

        if (ticker == null || ticker.isBlank()) {

            throw new IllegalArgumentException(
                    "Ticker cannot be empty."
            );
        }

        if (!ticker.matches("[A-Za-z]{1,10}")) {

            throw new IllegalArgumentException(
                    "Invalid ticker: " + ticker
            );
        }
    }


    private void validateId(
            Long id,
            String fieldName) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    fieldName +
                    " must be greater than zero."
            );
        }
    }
}
