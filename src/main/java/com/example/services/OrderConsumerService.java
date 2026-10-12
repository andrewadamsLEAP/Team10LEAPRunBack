package com.example.services;

import com.example.entities.Order;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.OrdersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumerService.class);

    private final OrdersRepository ordersRepository;
    private final HoldingsService holdingsService;
    private final ClientsService clientsService;
    private final ObjectMapper objectMapper;

    public OrderConsumerService(
            OrdersRepository ordersRepository,
            HoldingsService holdingsService,
            ClientsService clientsService) {

        this.ordersRepository = ordersRepository;
        this.holdingsService = holdingsService;
        this.clientsService = clientsService;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================
    //                    EXECUTE ORDER
    // =========================================================

    @KafkaListener(topics = "${app.kafka.topics.order-pending:order-pending-topic}", groupId = "trading-app-group")
    @Transactional
    public void consumeOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);
            logger.info("Consumed order from Kafka: {}", order.getOrderId());
            executeOrder(order);
        } catch (Exception e) {
            logger.error("Failed to consume or execute order: {}", orderJson, e);
        }
    }

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

        // Refetch the order from database to ensure enums are properly populated
        Order fulfilledOrder = getOrderById(orderId);

        // Update holdings when order is fulfilled
        holdingsService.updateHoldingsForOrder(fulfilledOrder);

        // Update client cash based on order type
        BigDecimal transactionAmount = fulfilledOrder.getPrice()
                .multiply(BigDecimal.valueOf(fulfilledOrder.getQuantity()));

        if (fulfilledOrder.getOrderType() == Order.OrderType.BUY) {
            // Subtract cash for buy orders (negative amount)
            clientsService.updateCashAmount(fulfilledOrder.getClientId(), transactionAmount.negate());
        } else if (fulfilledOrder.getOrderType() == Order.OrderType.SELL) {
            // Add cash for sell orders (positive amount)
            clientsService.updateCashAmount(fulfilledOrder.getClientId(), transactionAmount);
        }

        logger.info("Cash updated for order execution via Kafka: orderId={}, orderType={}, amount={}",
                orderId, fulfilledOrder.getOrderType(), transactionAmount);

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