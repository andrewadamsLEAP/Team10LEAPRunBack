package com.example.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Kafka configuration for local development when Kafka is not available.
 * Provides a mock KafkaTemplate bean to satisfy OrdersService dependencies.
 * This configuration is only loaded if no KafkaTemplate bean is already defined.
 */
@Configuration
public class LocalKafkaConfig {

    /**
     * Provides a fallback KafkaTemplate bean for local development.
     * This bean is only created if no other KafkaTemplate bean exists.
     */
    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        // Create a basic producer factory with minimal configuration
        // This allows the app to start without an active Kafka broker
        ProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(new HashMap<>());
        return new KafkaTemplate<>(producerFactory);
    }
}
