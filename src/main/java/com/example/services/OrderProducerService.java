package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.kafka.core.KafkaTemplate;
import com.example.entities.Order;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderProducerService {
    private static final Logger logger = LoggerFactory.getLogger(OrdersService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String orderPendingTopic;
    private final ObjectMapper objectMapper;

    public OrderProducerService (
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${app.kafka.topics.order-pending}") String orderPendingTopic,
        ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;    
        this.orderPendingTopic = orderPendingTopic;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void produceOrder(Order order) {
        logger.info("before try: {}", order.getOrderId());

        try {
            String orderJson = objectMapper.writeValueAsString(order);
            kafkaTemplate.send(orderPendingTopic, order.getTicker(), orderJson);
            logger.info("Successfully published order to Kafka: {}", order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to publish order to Kafka: " + order.getOrderId(), e);
        }
    }
    
}
