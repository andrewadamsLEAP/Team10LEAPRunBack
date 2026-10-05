package com.example.tradingApp;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class KafkaTestConfig {

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public KafkaTemplate kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}