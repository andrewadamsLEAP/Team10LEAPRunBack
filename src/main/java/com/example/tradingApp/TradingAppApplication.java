package com.example.tradingApp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.example"})
@MapperScan("com.example.mappers")
public class TradingAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TradingAppApplication.class,
                args);
    }
}