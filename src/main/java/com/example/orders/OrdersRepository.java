package com.example.orders;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrdersRepository {

    private final OrdersMapper ordersMapper;

    public OrdersRepository(OrdersMapper ordersMapper) {
        this.ordersMapper = ordersMapper;
    }

    // =========================================================
    //                      SEARCH FUNCTIONS
    // =========================================================

    public Order getOrderById(Long orderId) {

        return ordersMapper.getOrderById(orderId);
    }


    public List<Order> getFulfilledOrders(Long clientId) {

        return ordersMapper.getFulfilledOrders(clientId);
    }


    public List<Order> getCancelledOrders() {

        return ordersMapper.getCancelledOrders();
    }


    public List<Order> getPendingSellOrdersForTicker(
            String ticker) {

        return ordersMapper.getPendingSellOrdersForTicker(ticker);
    }


    public List<Order> getPendingBuyOrdersForTicker(
            String ticker) {

        return ordersMapper.getPendingBuyOrdersForTicker(ticker);
    }


    // =========================================================
    //                  ORDER MANIPULATION
    // =========================================================

    public Order createOrder(Order order) {

        ordersMapper.createOrder(order);

        return order;
    }


    public int updateOrderStatus(
            Long orderId,
            Order.OrderStatus status) {

        return ordersMapper.updateOrderStatus(
                orderId,
                status
        );
    }
}
