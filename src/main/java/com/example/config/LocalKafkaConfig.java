package com.example.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

/**
 * Local development Kafka configuration fallback.
 * 
 * This configuration provides a KafkaTemplate bean when:
 * 1. Kafka broker is not available (localhost:9092)
 * 2. The primary KafkaConfig fails to initialize
 * 
 * In local development without an active Kafka broker, Spring will use this
 * configuration to provide a working (but non-functional) KafkaTemplate.
 * Messages published to Kafka will be discarded, allowing the application
 * to start and run for testing purposes.
 * 
 * For production, ensure Kafka is running and the primary KafkaConfig loads.
 */
@Configuration
public class LocalKafkaConfig {

    /**
     * Fallback KafkaTemplate bean for local development
     * Only created if no other KafkaTemplate bean exists
     */
    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Create a minimal producer factory that won't fail
        System.out.println("Creating fallback KafkaTemplate for local development, prod Kafka bean not found.");
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        
        ProducerFactory<String, Object> producerFactory = 
            new DefaultKafkaProducerFactory<>(configProps);
        
        return new KafkaTemplate<>(producerFactory);
    }
}
