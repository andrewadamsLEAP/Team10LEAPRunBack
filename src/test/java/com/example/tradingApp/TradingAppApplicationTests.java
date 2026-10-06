package com.example.tradingApp;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ActiveProfiles("test")
@Import(KafkaTestConfig.class)
class TradingAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
