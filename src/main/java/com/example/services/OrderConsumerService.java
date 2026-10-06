package com.example.services;

import com.example.entities.Order;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.OrdersRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderConsumerService {

    private final OrdersRepository ordersRepository;
    private final HoldingsService holdingsService;

    public OrderConsumerService(
            OrdersRepository ordersRepository,
            HoldingsService holdingsService) {

        this.ordersRepository = ordersRepository;
        this.holdingsService = holdingsService;
    }

    // =========================================================
    //                    EXECUTE ORDER
    // =========================================================

    @KafkaListener(topics = "${app.kafka.topics.order-pending}")
    @Transactional
    public Order executeOrder(Order order) {
        Long orderId = order.getOrderId();

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

    private Order getOrderById(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "Order ID must be greater than zero."
            );
        }

        Order order = ordersRepository.getOrderById(orderId);

        if (order == null) {
            throw new InvalidArgumentsException(
                    "Order Not Found",
                    "Order not found: " + orderId
            );
        }

        return order;
    }
}
