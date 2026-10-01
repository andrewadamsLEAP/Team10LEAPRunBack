package com.example.tradingApp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.tradingApp.TradingAppApplication;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TradingAppApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:holdings-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class HoldingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==== GET HOLDING TESTS ====
    @Test
    void getHoldingReturnsHoldingResponseWhenFound() throws Exception {
        Long clientId = insertClient("test1@example.com", "testuser1", "password1", "Test", "User1", new BigDecimal("1000.00"));
        insertHolding(clientId, "AAPL", 100);

        mockMvc.perform(get("/api/v1/holdings/{clientId}/{ticker}", clientId, "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void getHoldingThrowsNotFoundWhenClientDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/holdings/{clientId}/{ticker}", 99999L, "AAPL"))
                .andExpect(status().isNotFound());
    }

    // ==== GET QUANTITY TESTS ====
    @Test
    void getQuantityReturnsQuantityResponseWhenFound() throws Exception {
        Long clientId = insertClient("test3@example.com", "testuser3", "password1", "Test", "User3", new BigDecimal("1000.00"));
        insertHolding(clientId, "MSFT", 50);

        mockMvc.perform(get("/api/v1/holdings/quantity")
                        .param("clientId", clientId.toString())
                        .param("ticker", "MSFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    void getQuantityThrowsNotFoundWhenClientNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/holdings/quantity")
                        .param("clientId", "99999")
                        .param("ticker", "AAPL"))
                .andExpect(status().isNotFound());
    }

    // ==== GET ALL CLIENT HOLDINGS TESTS ====
    @Test
    void getClientHoldingsReturnsListWhenPresent() throws Exception {
        Long clientId = insertClient("test5@example.com", "testuser5", "password1", "Test", "User5", new BigDecimal("1000.00"));
        insertHolding(clientId, "AAPL", 100);
        insertHolding(clientId, "MSFT", 50);

        mockMvc.perform(get("/api/v1/holdings/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[1].ticker").value("MSFT"));
    }

    @Test
    void getClientHoldingsReturnsEmptyListWhenNoHoldings() throws Exception {
        Long clientId = insertClient("test6@example.com", "testuser6", "password1", "Test", "User6", new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/v1/holdings/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getClientHoldingsThrowsNotFoundWhenClientNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/holdings/client/{clientId}", 99999L))
                .andExpect(status().isNotFound());
    }

    private Long insertClient(String email, String username, String password, String firstName, String lastName, BigDecimal cashAmount) {
        jdbcTemplate.update(
                "INSERT INTO clients (email, username, password, first_name, last_name, cash_amount) VALUES (?, ?, ?, ?, ?, ?)",
                email,
                username,
                password,
                firstName,
                lastName,
                cashAmount);

        return jdbcTemplate.queryForObject(
                "SELECT client_id FROM clients WHERE username = ?",
                Long.class,
                username);
    }

    private void insertHolding(Long clientId, String ticker, Integer quantity) {
        jdbcTemplate.update(
                "INSERT INTO holdings (client_id, ticker, quantity, updated_at) VALUES (?, ?, ?, CURRENT_DATE)",
                clientId,
                ticker,
                quantity);
    }
}
