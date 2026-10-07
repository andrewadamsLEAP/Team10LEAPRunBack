package com.example.tradingApp;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class KafkaTestConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> template = Mockito.mock(KafkaTemplate.class);
        return template;
    }
}