package com.example.integration;

import com.example.DTOs.orders.OrderResponse;
import com.example.DTOs.orders.OrderHistoryView;
import com.example.entities.Order;
import com.example.tradingApp.TradingAppApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Postman-style API Integration Tests
 * Tests all order endpoints as if calling from Postman or REST client
 * 
 * This mimics real API interactions:
 * - POST /orders/buy - Place buy order
 * - POST /orders/sell - Place sell order  
 * - GET /orders/{id} - Get order details
 * - GET /orders/client/{clientId}/fulfilled - Get fulfilled orders
 * - DELETE /orders/{id} - Cancel order
 * - Various error scenarios
 */
@SpringBootTest(classes = TradingAppApplication.class)
@AutoConfigureMockMvc
@DisplayName("Orders API Integration Tests (Postman-style)")
class OrdersAPIIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Long testClientId = 1L;
    private String testTicker = "AAPL";
    private int testQuantity = 10;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Nested
    @DisplayName("BUY ORDER Endpoints")
    class BuyOrderEndpoints {

        @Test
        @DisplayName("POST /orders/buy - Should create buy order successfully")
        void placeBuyOrderSuccess() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, testQuantity);

            // Act & Assert
            MvcResult result = mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").isNumber())
                    .andExpect(jsonPath("$.clientId").value(testClientId))
                    .andExpect(jsonPath("$.ticker").value(testTicker.toUpperCase()))
                    .andExpect(jsonPath("$.type").value("BUY"))
                    .andExpect(jsonPath("$.quantity").value(testQuantity))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            System.out.println("✅ Buy order created: " + result.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("POST /orders/buy - Should reject with invalid quantity")
        void placeBuyOrderInvalidQuantity() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, 0);  // Invalid quantity

            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /orders/buy - Should reject with invalid ticker")
        void placeBuyOrderInvalidTicker() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, "INVALID_TICKER_12345", testQuantity);

            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /orders/buy - Should reject with non-existent client")
        void placeBuyOrderNonExistentClient() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, 99999L, testTicker, testQuantity);

            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("SELL ORDER Endpoints")
    class SellOrderEndpoints {

        @Test
        @DisplayName("POST /orders/sell - Should create sell order successfully")
        void placeSellOrderSuccess() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, 5);

            // Act & Assert
            mockMvc.perform(post("/orders/sell")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").isNumber())
                    .andExpect(jsonPath("$.type").value("SELL"))
                    .andExpect(jsonPath("$.quantity").value(5))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /orders/sell - Should reject insufficient holdings")
        void placeSellOrderInsufficientHoldings() throws Exception {
            // Arrange: Try to sell more than client owns
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, "GOOG", 10000);

            // Act & Assert
            mockMvc.perform(post("/orders/sell")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("ORDER RETRIEVAL Endpoints")
    class OrderRetrievalEndpoints {

        @Test
        @DisplayName("GET /orders/{orderId} - Should retrieve order by ID")
        void getOrderById() throws Exception {
            // Arrange: First create an order
            String createRequest = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, testQuantity);

            MvcResult createResult = mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
                    .andExpect(status().isOk())
                    .andReturn();

            // Extract orderId from response
            String responseBody = createResult.getResponse().getContentAsString();
            Long orderId = extractOrderId(responseBody);

            // Act & Assert: Retrieve the order
            mockMvc.perform(get("/orders/" + orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.clientId").value(testClientId))
                    .andExpect(jsonPath("$.ticker").value(testTicker.toUpperCase()));
        }

        @Test
        @DisplayName("GET /orders/{orderId} - Should return 404 for non-existent order")
        void getOrderNotFound() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/orders/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /orders/client/{clientId}/fulfilled - Should list fulfilled orders")
        void getFulfilledOrders() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/orders/client/" + testClientId + "/fulfilled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("GET /orders/client/{clientId}/fulfilled - Should return empty list for new client")
        void getFulfilledOrdersEmpty() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/orders/client/99999/fulfilled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("CANCEL ORDER Endpoints")
    class CancelOrderEndpoints {

        @Test
        @DisplayName("DELETE /orders/{orderId} - Should cancel pending order")
        void cancelOrderSuccess() throws Exception {
            // Arrange: Create an order first
            String createRequest = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, testQuantity);

            MvcResult createResult = mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
                    .andExpect(status().isOk())
                    .andReturn();

            Long orderId = extractOrderId(createResult.getResponse().getContentAsString());

            // Act & Assert: Cancel the order
            mockMvc.perform(delete("/orders/" + orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("DELETE /orders/{orderId} - Should reject cancelling already fulfilled order")
        void cancelOrderAlreadyFulfilled() throws Exception {
            // This test assumes the order is already fulfilled (via Kafka)
            // After some delay, the order will be FULFILLED
            Long orderId = 1L;  // Assume this order is fulfilled

            // Act & Assert
            mockMvc.perform(delete("/orders/" + orderId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("ERROR HANDLING")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle malformed JSON request")
        void malformedJsonRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{malformed json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing required fields")
        void missingRequiredFields() throws Exception {
            // Arrange: Missing quantity
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s"
                }
                """, testClientId, testTicker);

            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle negative quantity")
        void negativeQuantity() throws Exception {
            // Arrange
            String requestBody = String.format("""
                {
                    "clientId": %d,
                    "ticker": "%s",
                    "quantity": %d
                }
                """, testClientId, testTicker, -5);

            // Act & Assert
            mockMvc.perform(post("/orders/buy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    // Helper methods
    private Long extractOrderId(String jsonResponse) {
        // Parse JSON and extract orderId
        // In real tests, use JsonPath or similar
        try {
            int startIdx = jsonResponse.indexOf("\"orderId\":") + 10;
            int endIdx = jsonResponse.indexOf(",", startIdx);
            if (endIdx == -1) endIdx = jsonResponse.indexOf("}", startIdx);
            return Long.parseLong(jsonResponse.substring(startIdx, endIdx).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
