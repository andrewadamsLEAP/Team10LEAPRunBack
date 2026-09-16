package com.example.tradingApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

// The main class for the TradingApp application. This class is responsible for bootstrapping the Spring Boot application and enabling scheduling for periodic tasks.
@EnableScheduling 
@SpringBootApplication
@ComponentScan(basePackages = {"com.example"})
@MapperScan("com.example.repositories")
public class TradingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingAppApplication.class, args);
	}

}
