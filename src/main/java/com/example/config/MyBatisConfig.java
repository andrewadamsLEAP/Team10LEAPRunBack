package com.example.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
    "com.example.repositories",
    "com.example.holdings",
    "com.example.orders",
    "com.example.marketData"
})
public class MyBatisConfig {
    // This configuration explicitly scans for MyBatis mappers
    // in all the packages where we have @Mapper interfaces
}