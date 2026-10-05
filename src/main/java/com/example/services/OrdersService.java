package com.example.services;
import com.example.entities.Order;
import com.example.entities.Instrument;
import com.example.repositories.OrdersRepository;
import com.example.services.MarketHoursService;
import com.example.services.HoldingsService;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import com.example.exceptions.InvalidArgumentsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(OrdersService.class);

    private final OrdersRepository ordersRepository;
    private final MarketHoursService marketHoursService;
    private final HoldingsService holdingsService;
    private final OrderDtoConverter orderDtoConverter;
    private final ClientsService clientsService;
    private final InstrumentService instrumentService;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String orderPendingTopic;

    public OrdersService(
            OrdersRepository ordersRepository,
            MarketHoursService marketHoursService,
            HoldingsService holdingsService,
            OrderDtoConverter orderDtoConverter,
            ClientsService clientsService,
            InstrumentService instrumentService,
            KafkaTemplate<Object, Object> kafkaTemplate,
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

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the ID of the order to retrieve
     * @return the Order object with the specified ID
     * @throws InvalidArgumentsException if order is not found
     * @throws IllegalArgumentException if orderId is invalid
     */
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

    /**
     * Retrieves all fulfilled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of fulfilled orders for the specified client
     * @throws IllegalArgumentException if clientId is invalid
     */
    public List<Order> getFulfilledOrders(Long clientId) {

        validateId(clientId, "Client ID");

        return ordersRepository.getFulfilledOrders(clientId);
    }

    /**
     * Retrieves all cancelled orders across all clients.
     *
     * @return a list of all cancelled orders
     */
    public List<Order> getCancelledOrders() {

        return ordersRepository.getCancelledOrders();
    }

    /**
     * Retrieves all cancelled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of cancelled orders for the specified client
     * @throws IllegalArgumentException if clientId is invalid
     */
    public List<Order> getCancelledOrdersForClient(Long clientId) {

        validateId(clientId, "Client ID");

        return ordersRepository.getCancelledOrdersForClient(clientId);
    }

    /**
     * Retrieves all pending sell orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending sell orders for the specified ticker
     * @throws IllegalArgumentException if ticker format is invalid
     */
    public List<Order> getPendingSellOrdersForTicker(
            String ticker) {

        validateTicker(ticker);

        return ordersRepository.getPendingSellOrdersForTicker(
                ticker.toUpperCase()
        );
    }

    /**
     * Retrieves all pending buy orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending buy orders for the specified ticker
     * @throws IllegalArgumentException if ticker format is invalid
     */
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

    /**
     * Places a buy order for a client to purchase shares/crypto.
     * Validates sufficient cash and applies market hours restrictions for stocks.
     *
     * @param clientId the ID of the client placing the order
     * @param ticker the ticker symbol of the asset to buy
     * @param quantity the number of shares/units to buy (must be > 0)
     * @param price the price per share/unit (must be > 0)
     * @return the created Order object with PENDING status
     * @throws IllegalArgumentException if validation fails (insufficient cash, invalid ticker, etc.)
     * @throws IllegalStateException if market is closed for stocks or order cannot be created
     */
    @Transactional
    public Order placeBuyOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        logger.info("Place buy order: clientId={}, ticker={}, quantity={}, price={}", clientId, ticker, quantity, price);
        validateBuyOrder(
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
        logger.info("Buy order created: orderId={}, clientId={}, ticker={}", createdOrder.getOrderId(), clientId, ticker);
        publishOrderAfterCommit(createdOrder);
        return createdOrder;
    }


    // =========================================================
    //                  PLACE SELL ORDER
    // =========================================================

    /**
     * Places a sell order for a client to sell their holdings.
     * Validates sufficient holdings and applies market hours restrictions for stocks.
     *
     * @param clientId the ID of the client placing the order
     * @param ticker the ticker symbol of the asset to sell
     * @param quantity the number of shares/units to sell (must be > 0)
     * @param price the price per share/unit (must be > 0)
     * @return the created Order object with PENDING status
     * @throws IllegalArgumentException if validation fails (insufficient holdings, invalid ticker, etc.)
     * @throws IllegalStateException if market is closed for stocks or order cannot be created
     */
    @Transactional
    public Order placeSellOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        logger.info("Place sell order: clientId={}, ticker={}, quantity={}, price={}", clientId, ticker, quantity, price);
        
        validateSellOrder(
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
        logger.info("Sell order created: orderId={}, clientId={}, ticker={}", createdOrder.getOrderId(), clientId, ticker);
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

    /**
     * Cancels a pending order. Only pending orders can be cancelled.
     * Once cancelled, an order cannot be executed.
     *
     * @param orderId the ID of the order to cancel
     * @return the cancelled Order object with CANCELLED status
     * @throws InvalidArgumentsException if order is not found
     * @throws IllegalStateException if order status is not PENDING
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        logger.info("Cancel order request: orderId={}", orderId);

        Order order = getOrderById(orderId);

        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            logger.warn("Cannot cancel order: orderId={}, status={}", orderId, order.getOrderStatus());
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
    //                    EXECUTE ORDER
    // =========================================================

    /**
     * Executes a pending order, changing its status to FULFILLED and updating client holdings.
     * Only pending orders can be executed.
     *
     * @param orderId the ID of the order to execute
     * @return the executed Order object with FULFILLED status
     * @throws InvalidArgumentsException if order is not found
     * @throws IllegalStateException if order status is not PENDING
     */
    @Transactional
    public Order executeOrder(Long orderId) {

        Order order = getOrderById(orderId);

        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {

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

        Order fulfilledOrder = getOrderById(orderId);
        
        // Update holdings when order is fulfilled
        holdingsService.updateHoldingsForOrder(fulfilledOrder);
        
        return fulfilledOrder;
    }


    // =========================================================
    //                       VALIDATION
    // =========================================================

    /**
     * Common validation for all orders (stocks, crypto, forex).
     * Checks format validity, client existence, instrument existence, and asset-specific rules.
     * Asset type determines if market hours validation applies:
     * - Stocks: must be during US market hours
     * - Crypto/Forex: trades 24/7, no market hours restriction
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the number of shares/units
     * @param price the price per share/unit
     * @throws IllegalArgumentException if any validation fails
     * @throws IllegalStateException if market is closed for stocks or other state violations
     */
    private void validateOrderCommon(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        validateId(clientId, "Client ID");
        validateTicker(ticker);
        
        // VALIDATION: Verify ticker exists and get instrument details
        Instrument instrument = instrumentService.getInstrumentByTicker(ticker);
        if (instrument == null) {
            throw new IllegalArgumentException(
                    "Instrument not found: " + ticker
            );
        }

        // VALIDATION: Verify client exists in database
        try {
            clientsService.getClientProfile(clientId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Client not found: " + clientId
            );
        }

        // Market hours validation only applies to stocks
        if ("STOCK".equalsIgnoreCase(instrument.getAssetType()) 
                && !marketHoursService.isUsMarketHours()) {
            throw new IllegalStateException(
                    "Orders can only be placed during US market hours."
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

    /**
     * Validation for BUY orders: checks basic order validity plus
     * whether the client has sufficient cash to purchase the shares.
     * Accounts for pending buy orders when calculating available cash.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the number of shares/units to buy
     * @param price the price per share/unit
     * @throws IllegalArgumentException if validation fails (insufficient cash, etc.)
     * @throws IllegalStateException if other validation fails
     */
    private void validateBuyOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        // First, validate basic order requirements
        validateOrderCommon(clientId, ticker, quantity, price);

        // VALIDATION: Verify client has enough cash
        BigDecimal orderCost = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal clientCash = clientsService.getClientProfile(clientId).cashAmount();

        List<Order> pendingBuyOrders = ordersRepository.getPendingBuyOrdersForClient(clientId);
        for(int i = 0; i < pendingBuyOrders.size(); i++) {
            orderCost = orderCost.add(pendingBuyOrders.get(i).getPrice().multiply(BigDecimal.valueOf(pendingBuyOrders.get(i).getQuantity())));
        }

        if (clientCash.compareTo(orderCost) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient cash. Client has $" + clientCash +
                    " but order costs $" + orderCost
            );
        }
    }

    /**
     * Validation for SELL orders: checks basic order validity plus
     * whether the client has sufficient shares to sell.
     * Accounts for pending sell orders when calculating available shares.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the number of shares/units to sell
     * @param price the price per share/unit
     * @throws IllegalArgumentException if validation fails (insufficient holdings, etc.)
     * @throws IllegalStateException if other validation fails
     */
    private void validateSellOrder(
            Long clientId,
            String ticker,
            int quantity,
            BigDecimal price) {

        // First, validate basic order requirements
        validateOrderCommon(clientId, ticker, quantity, price);

        List<Order> pendingSellOrders = ordersRepository.getPendingSellOrdersForClientAndTicker(clientId, ticker.toUpperCase());
        int reservedShares = 0;
        for(int i = 0; i < pendingSellOrders.size(); i++) {
            reservedShares += pendingSellOrders.get(i).getQuantity();
        }

        // VALIDATION: Verify client has enough shares to sell
        try {
            com.example.DTOs.holdings.HoldingResponse holding = 
                holdingsService.getHolding(clientId, ticker.toUpperCase());
            
            if (holding.quantity() - reservedShares < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient holdings. Client has " + holding.quantity() +
                        " shares of " + ticker + " but " + reservedShares + " are reserved and trying to sell " + quantity
                );
            }
        } catch (IllegalArgumentException e) {
            // Re-throw IllegalArgumentException as is
            throw e;
        } catch (Exception e) {
            // If holding doesn't exist, client has no shares
            throw new IllegalArgumentException(
                    "Client does not own any shares of " + ticker
            );
        }
    }

    /**
     * Validates that a ticker is in valid format.
     * Valid ticker must be 1-10 alphabetic characters (case-insensitive).
     *
     * @param ticker the ticker symbol to validate
     * @throws IllegalArgumentException if ticker is null, empty, or invalid format
     */
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

    /**
     * Validates that an ID is positive and non-null.
     * ID must be greater than zero.
     *
     * @param id the ID to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws IllegalArgumentException if ID is null or not greater than zero
     */
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
     * Retrieves an order by ID and converts it to a DTO response.
     *
     * @param orderId the ID of the order to retrieve
     * @return an OrderResponse DTO containing the order data
     * @throws InvalidArgumentsException if order is not found
     * @throws IllegalArgumentException if orderId is invalid
     */
    public OrderResponse getOrderByIdAsDto(Long orderId) {
        Order order = getOrderById(orderId);
        return orderDtoConverter.toOrderResponse(order);
    }

    /**
     * Retrieves all fulfilled orders for a client and converts them to DTOs.
     *
     * @param clientId the ID of the client
     * @return a list of OrderHistoryView DTOs for fulfilled orders
     * @throws IllegalArgumentException if clientId is invalid
     */
    public List<OrderHistoryView> getFulfilledOrdersAsDto(Long clientId) {
        List<Order> orders = getFulfilledOrders(clientId);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Retrieves all cancelled orders across all clients and converts them to DTOs.
     *
     * @return a list of OrderHistoryView DTOs for all cancelled orders
     */
    public List<OrderHistoryView> getCancelledOrdersAsDto() {
        List<Order> orders = getCancelledOrders();
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Retrieves all cancelled orders for a specific client and converts them to DTOs.
     *
     * @param clientId the ID of the client
     * @return a list of OrderHistoryView DTOs for cancelled orders
     * @throws IllegalArgumentException if clientId is invalid
     */
    public List<OrderHistoryView> getCancelledOrdersForClientAsDto(Long clientId) {
        List<Order> orders = getCancelledOrdersForClient(clientId);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Retrieves all pending sell orders for a ticker and converts them to DTOs.
     *
     * @param ticker the ticker symbol
     * @return a list of OrderHistoryView DTOs for pending sell orders
     * @throws IllegalArgumentException if ticker format is invalid
     */
    public List<OrderHistoryView> getPendingSellOrdersForTickerAsDto(String ticker) {
        List<Order> orders = getPendingSellOrdersForTicker(ticker);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Retrieves all pending buy orders for a ticker and converts them to DTOs.
     *
     * @param ticker the ticker symbol
     * @return a list of OrderHistoryView DTOs for pending buy orders
     * @throws IllegalArgumentException if ticker format is invalid
     */
    public List<OrderHistoryView> getPendingBuyOrdersForTickerAsDto(String ticker) {
        List<Order> orders = getPendingBuyOrdersForTicker(ticker);
        return orderDtoConverter.toOrderHistoryViews(orders);
    }

    /**
     * Places a buy order for a client and returns the result as a DTO.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the number of shares/units to buy
     * @param price the price per share/unit
     * @return an OrderResponse DTO with the created order data
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if market is closed or order cannot be created
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
     * Places a sell order for a client and returns the result as a DTO.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @param quantity the number of shares/units to sell
     * @param price the price per share/unit
     * @return an OrderResponse DTO with the created order data
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if market is closed or order cannot be created
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
     * Cancels an order and returns the result as a DTO.
     *
     * @param orderId the ID of the order to cancel
     * @return an OrderResponse DTO with the cancelled order data
     * @throws InvalidArgumentsException if order is not found
     * @throws IllegalStateException if order status is not PENDING
     */
    public OrderResponse cancelOrderAsDto(Long orderId) {
        Order order = cancelOrder(orderId);
        return orderDtoConverter.toOrderResponse(order);
    }
}
