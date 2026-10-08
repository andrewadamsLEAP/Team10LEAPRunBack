package com.example.config;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.entities.Order;

@Configuration
@MapperScan(basePackages = "com.example.mappers")
public class MyBatisConfig {
    
    @Bean
    public ConfigurationCustomizer typeHandlerCustomizer() {
        return configuration -> {
            configuration.getTypeHandlerRegistry().register(Order.OrderType.class, new OrderTypeHandler());
            configuration.getTypeHandlerRegistry().register(Order.OrderStatus.class, new OrderStatusHandler());
        };
    }
}

