package com.example.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.example.mappers")
public class MyBatisConfig {
    // This configuration explicitly scans for MyBatis mappers
    // in the mappers package where we have @Mapper interfaces
}

