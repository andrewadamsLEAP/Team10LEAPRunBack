package com.example.services;
import com.example.entities.Order;
import com.example.repositories.OrdersRepository;
import com.example.services.MarketHoursService;
import com.example.services.HoldingsService;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import com.example.exceptions.InvalidArgumentsException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final MarketHoursService marketHoursService;
    private final HoldingsService holdingsService;
    private final OrderDtoConverter orderDtoConverter;
    private final ClientsService clientsService;
    private final InstrumentService instrumentService;
    private final KafkaTemplate kafkaTemplate;
    private final String orderPendingTopic;

    public OrdersService(
            OrdersRepository ordersRepository,
            MarketHoursService marketHoursService,
            HoldingsService holdingsService,
            OrderDtoConverter orderDtoConverter,
            ClientsService clientsService,
            InstrumentService instrumentService,
            KafkaTemplate kafkaTemplate,
            @Value("${app.kafka.topics.order-pending}") String orderPendingTopic
        ) {

        this.ordersRepository = ordersRepository;
        this.marketHoursService = marketHoursService;
        this.holdingsService = holdingsService;
        this.orderDtoConverter = orderDtoConverter;
        this.clientsService = clientsService;
        this.instrumentService = instrumentService;
        this.kafkaTemplate = kafkaTemplate;
        this.orderPendingTopic = orderPendingTopic;
    }

    // =========================================================
    //                      SEARCH FUNCTIONS
    // =========================================================

    public Order getOrderById(Long orderId) {

        validateId(orderId, "Order ID");

        Order order = ordersRepository.getOrderById(orderId);

        if (order == null) {
            throw new InvalidArgumentsException(
                    "Order Not Found",
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

    public List<Order> getCancelledOrdersForClient(Long clientId) {

        validateId(clientId, "Client ID");

        return ordersRepository.getCancelledOrdersForClient(clientId);
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

        Order createdOrder = ordersRepository.createOrder(order);
        publishOrderAfterCommit(createdOrder);

        return createdOrder;
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

        Order createdOrder = ordersRepository.createOrder(order);
        publishOrderAfterCommit(createdOrder);

        return createdOrder;
    }

    // =========================================================
    //                   PUBLISH ORDER HELPER
    // =========================================================
    public void publishOrderAfterCommit(Order order) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Order not actively in a transaction.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(orderPendingTopic, order.getTicker(), order);
            }
        });
    }

    // =========================================================
    //                       CANCEL ORDER
    // =========================================================

    @Transactional
    public Order cancelOrder(Long orderId) {

        Order order = getOrderById(orderId);

        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {

            throw new IllegalStateException(
                    "Order " + orderId +
                    " cannot be cancelled because it is " +
                    order.getOrderStatus()
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
        
        // VALIDATION: Verify client exists in database
        try {
            clientsService.getClientProfile(clientId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Client not found: " + clientId
            );
        }

        validateTicker(ticker);
        
        // VALIDATION: Verify ticker exists in instruments table
        if (!instrumentService.instrumentExists(ticker)) {
            throw new IllegalArgumentException(
                    "Instrument not found: " + ticker
            );
        }

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

    // =========================================================
    //                  DTO CONVERSION METHODS
    //              (For API responses - controller use)
    // =========================================================

    /**
     * Get order by ID and return as DTO
     */
    public OrderResponse getOrderByIdAsDto(Long orderId) {
        Order order = getOrderById(orderId);
        return orderDtoConverter.toOrderResponse(order);
    }

    /**
     * Get fulfilled orders and return as DTOs
     */
    public List<OrderHistoryView> getFulfilledOrdersAsDto(Long clientId) {
        List<Order> orders = getFulfilledOrders(clientId);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Get cancelled orders and return as DTOs
     */
    public List<OrderHistoryView> getCancelledOrdersAsDto() {
        List<Order> orders = getCancelledOrders();
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Get cancelled orders for client and return as DTOs
     */
    public List<OrderHistoryView> getCancelledOrdersForClientAsDto(Long clientId) {
        List<Order> orders = getCancelledOrdersForClient(clientId);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Get pending sell orders for ticker and return as DTOs
     */
    public List<OrderHistoryView> getPendingSellOrdersForTickerAsDto(String ticker) {
        List<Order> orders = getPendingSellOrdersForTicker(ticker);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Get pending buy orders for ticker and return as DTOs
     */
    public List<OrderHistoryView> getPendingBuyOrdersForTickerAsDto(String ticker) {
        List<Order> orders = getPendingBuyOrdersForTicker(ticker);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Place buy order and return as DTO
     */
    public OrderResponse placeBuyOrderAsDto(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {
        Order order = placeBuyOrder(clientId, ticker, quantity, price);
        return orderDtoConverter.toOrderResponse(order);
    }

    /**
     * Place sell order and return as DTO
     */
    public OrderResponse placeSellOrderAsDto(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {
        Order order = placeSellOrder(clientId, ticker, quantity, price);
        return orderDtoConverter.toOrderResponse(order);
    }

    /**
     * Cancel order and return as DTO
     */
    public OrderResponse cancelOrderAsDto(Long orderId) {
        Order order = cancelOrder(orderId);
        return orderDtoConverter.toOrderResponse(order);
    }
}
