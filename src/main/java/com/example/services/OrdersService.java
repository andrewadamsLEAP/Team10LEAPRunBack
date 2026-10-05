package com.example.services;
import com.example.entities.Order;
import com.example.repositories.OrdersRepository;
import com.example.services.MarketHoursService;
import com.example.services.HoldingsService;
import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import com.example.exceptions.InvalidArgumentsException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OrdersService(
            OrdersRepository ordersRepository,
            MarketHoursService marketHoursService,
            HoldingsService holdingsService,
            OrderDtoConverter orderDtoConverter,
            ClientsService clientsService,
            InstrumentService instrumentService) {

        this.ordersRepository = ordersRepository;
        this.marketHoursService = marketHoursService;
        this.holdingsService = holdingsService;
        this.orderDtoConverter = orderDtoConverter;
        this.clientsService = clientsService;
        this.instrumentService = instrumentService;
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

        return ordersRepository.createOrder(order);
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
    //                    EXECUTE ORDER
    // =========================================================

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
     * Validation for BUY orders: checks basic order validity plus
     * whether the client has sufficient cash to purchase the shares.
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
     * Common validation for all orders
     */
    private void validateOrderCommon(
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
