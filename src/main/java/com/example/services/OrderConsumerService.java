package com.example.services;

import com.example.entities.Order;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.OrdersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumerService.class);

    private final OrdersRepository ordersRepository;
    private final HoldingsService holdingsService;
    private final ObjectMapper objectMapper;

    public OrderConsumerService(
            OrdersRepository ordersRepository,
            HoldingsService holdingsService) {

        this.ordersRepository = ordersRepository;
        this.holdingsService = holdingsService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================
    //                    EXECUTE ORDER
    // =========================================================

    @KafkaListener(topics = "${app.kafka.topics.order-pending:order-pending-topic}", groupId = "trading-app-group")
    @Transactional
    public void consumeOrder(String orderJson) {
        logger.info("======== KAFKA LISTENER TRIGGERED ========");
        logger.info("Received order message from Kafka topic: {}", "${app.kafka.topics.order-pending:order-pending-topic}");
        logger.debug("Raw order JSON: {}", orderJson);
        
        try {
            logger.info("Deserializing order JSON...");
            Order order = objectMapper.readValue(orderJson, Order.class);
            logger.info("Order deserialized successfully: orderId={}, clientId={}, ticker={}, type={}, quantity={}, status={}", 
                       order.getOrderId(), order.getClientId(), order.getTicker(), order.getOrderType(), 
                       order.getQuantity(), order.getOrderStatus());
            
            logger.info("Calling executeOrder() for orderId={}", order.getOrderId());
            executeOrder(order);
            logger.info("Order execution completed successfully: orderId={}", order.getOrderId());
            logger.info("======== KAFKA LISTENER COMPLETED SUCCESSFULLY ========");
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Failed to deserialize order JSON: {}. JSON content: {}", e.getMessage(), orderJson, e);
        } catch (Exception e) {
            logger.error("Failed to consume or execute order: {}. JSON content: {}. Error: {}", 
                        e.getMessage(), orderJson, e.getClass().getSimpleName(), e);
            logger.error("======== KAFKA LISTENER FAILED ========");
        }
    }

    public Order executeOrder(Order order) {
        Long orderId = order.getOrderId();
        logger.info("========================================");
        logger.info("EXECUTING ORDER: orderId={}", orderId);
        logger.info("Order details: clientId={}, ticker={}, type={}, quantity={}, price={}, status={}", 
                   order.getClientId(), order.getTicker(), order.getOrderType(), order.getQuantity(), 
                   order.getPrice(), order.getOrderStatus());
        logger.info("========================================");

        // Validate order status
        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            logger.error("Order execution FAILED: orderId={} has status {} (expected PENDING). Cannot execute.", 
                        orderId, order.getOrderStatus());
            throw new IllegalStateException(
                    "Only pending orders can be executed."
            );
        }
        logger.debug("Order status validation passed: orderId={}", orderId);

        // Update order status in database
        logger.info("Updating order status to FULFILLED: orderId={}", orderId);
        int updated = ordersRepository.updateOrderStatus(
                orderId,
                Order.OrderStatus.FULFILLED
        );

        if (updated == 0) {
            logger.error("Order execution FAILED: orderId={} could not be updated to FULFILLED status", orderId);
            throw new IllegalStateException(
                    "Order " + orderId +
                            " is no longer pending and could not be executed."
            );
        }
        logger.info("Order status successfully updated to FULFILLED: orderId={}", orderId);

        // Fetch updated order from database
        logger.info("Fetching updated order from database: orderId={}", orderId);
        Order fulfilledOrder = getOrderById(orderId);
        logger.debug("Updated order retrieved: status={}, price={}", fulfilledOrder.getOrderStatus(), fulfilledOrder.getPrice());

        // Update holdings for the client
        logger.info("Updating holdings for client: clientId={}, ticker={}, type={}, quantity={}", 
                   fulfilledOrder.getClientId(), fulfilledOrder.getTicker(), fulfilledOrder.getOrderType(), 
                   fulfilledOrder.getQuantity());
        try {
            holdingsService.updateHoldingsForOrder(fulfilledOrder);
            logger.info("Holdings updated successfully: orderId={}", orderId);
        } catch (Exception e) {
            logger.error("Failed to update holdings for order: orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }

        logger.info("========================================");
        logger.info("ORDER EXECUTION COMPLETED SUCCESSFULLY: orderId={}", orderId);
        logger.info("========================================");
        return fulfilledOrder;
    }

    private Order getOrderById(Long orderId) {
        logger.debug("Retrieving order from database: orderId={}", orderId);

        if (orderId == null || orderId <= 0) {
            logger.error("Invalid order ID: orderId={}", orderId);
            throw new IllegalArgumentException(
                    "Order ID must be greater than zero."
            );
        }

        Order order = ordersRepository.getOrderById(orderId);

        if (order == null) {
            logger.error("Order not found in database: orderId={}", orderId);
            throw new InvalidArgumentsException(
                    "Order Not Found",
                    "Order not found: " + orderId
            );
        }
        
        logger.debug("Order retrieved from database: orderId={}, status={}, ticker={}", 
                    orderId, order.getOrderStatus(), order.getTicker());
        return order;
    }
}