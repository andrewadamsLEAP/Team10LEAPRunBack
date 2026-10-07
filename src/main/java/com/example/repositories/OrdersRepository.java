package com.example.repositories;

import com.example.entities.Order;
import com.example.mappers.OrdersMapper;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class OrdersRepository {

    /**
     * The OrdersMapper instance used to interact with the database.
     */
    private final OrdersMapper ordersMapper;

    /**
     * Constructs a new OrdersRepository with the specified OrdersMapper.
     *
     * @param ordersMapper the OrdersMapper instance to use
     */
    public OrdersRepository(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    // =========================================================
    //                      SEARCH FUNCTIONS
    // =========================================================

    /**
     * Get order by ID.
     *
     * @param orderId the ID of the order to retrieve
     * @return the order with the specified ID
     */
    public Order getOrderById(Long orderId) {

        return ordersMapper.getOrderById(orderId);
    }


    /**
     * Get fulfilled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of fulfilled orders for the specified client
     */
    public List<Order> getFulfilledOrders(Long clientId) {

        return ordersMapper.getFulfilledOrders(clientId);
    }


    /**
     * Get cancelled orders.
     *
     * @return a list of all cancelled orders
     */
    public List<Order> getCancelledOrders() {

        return ordersMapper.getCancelledOrders();
    }

    /**
     * Get cancelled orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of cancelled orders for the specified client
     */
    public List<Order> getCancelledOrdersForClient(Long clientId) {

        return ordersMapper.getCancelledOrdersForClient(clientId);
    }

    /**
     * Get pending sell orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending sell orders for the specified ticker
     */
    public List<Order> getPendingSellOrdersForTicker(
            String ticker) {

        return ordersMapper.getPendingSellOrdersForTicker(ticker);
    }

    /**
     * Get pending sell orders for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of pending sell orders for the specified client
     */
    public List<Order> getPendingSellOrdersForClient(Long clientId) {

        return ordersMapper.getPendingSellOrdersForClient(clientId);
    }

    /**
     * Get pending sell orders for a specific client and ticker.
     * NEEDED for validating sell orders against reserved shares for correct order placement.
     *
     * @param clientId the ID of the client
     * @param ticker the ticker symbol
     * @return a list of pending sell orders for the specified client and ticker
     */
    public List<Order> getPendingSellOrdersForClientAndTicker(Long clientId, String ticker) {

        return ordersMapper.getPendingSellOrdersForClientAndTicker(clientId, ticker);
    }

    /**
     * Get pending buy orders for a specific ticker.
     *
     * @param ticker the ticker symbol
     * @return a list of pending buy orders for the specified ticker
     */
    public List<Order> getPendingBuyOrdersForTicker(
            String ticker) {

        return ordersMapper.getPendingBuyOrdersForTicker(ticker);
    }

    /**
     * Get all pending buy orders for a specific client.
     * 
     * @param clientId
     * @return
     */
    public List<Order> getPendingBuyOrdersForClient(Long clientId) {

        return ordersMapper.getPendingBuyOrdersForClient(clientId);
    }

    /**
     * Get ALL pending orders across all clients (both buy and sell).
     * Used by the order execution scheduler to find orders ready to execute.
     *
     * @return a list of all pending orders
     */
    public List<Order> getPendingOrders() {

        return ordersMapper.getPendingOrders();
    }


    /**
     * Create an order in the database.
     *
     * @param order the order to create
     * @return the created order
     */
    public Order createOrder(Order order) {

        ordersMapper.createOrder(order);

        return order;
    }


    /**
     * Update the status of an existing order.
     *
     * @param orderId the ID of the order to update
     * @param status the new status of the order
     * @return the number of rows affected
     */
    public int updateOrderStatus(
            Long orderId,
            Order.OrderStatus status) {

        return ordersMapper.updateOrderStatus(
                orderId,
                status
        );
    }

    /**
     * Update the execution price of an order (used when executing at current market price).
     *
     * @param orderId the ID of the order to update
     * @param price the new execution price
     * @return the number of rows affected
     */
    public int updateOrderPrice(Long orderId, BigDecimal price) {
        return ordersMapper.updateOrderPrice(orderId, price);
    }
}
