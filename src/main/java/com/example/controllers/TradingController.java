package com.example.controllers;

import com.example.services.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading")
public class TradingController {

    //Auto config for postgres database connection using Spring Boot's JdbcTemplate
    @Autowired
    private TradingService tradingService;



    // Example - GET http://localhost:8085/api/trading/test
    // Test endpoint to verify trading controller is working
    @GetMapping("/test")
    public String test() {
        try {
            return "Test endpoint is working!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing sample query: " + e.getMessage();
        }
    }

    // Example - GET http://localhost:8085/api/trading/orders/{clientId} 
    // Get all orders for a specific client
    @GetMapping("/orders/{clientId}")
    public List<Map<String, Object>> getOrdersByClient(@PathVariable Long clientId) {
        return tradingService.getOrdersByClient(clientId);
    }
}
