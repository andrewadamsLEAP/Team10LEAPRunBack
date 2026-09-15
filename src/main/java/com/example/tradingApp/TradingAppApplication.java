package com.example.tradingApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

// idk what this is doing
//@ComponentScan("com.example")
//@EnableJpaRepositories(basePackages = "com.example.repositories")
public class TradingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingAppApplication.class, args);
	}

}
