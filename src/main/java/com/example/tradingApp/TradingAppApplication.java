package com.example.tradingApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example"})
public class TradingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingAppApplication.class, args);
	}

}
