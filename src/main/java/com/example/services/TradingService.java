package com.example.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class TradingService {

    //Auto config for postgres database connection using Spring Boot's JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;


    //Example - Get all orders for a specific client
    public List<Map<String, Object>> getOrdersByClient(Long clientId) {
        String sql = "SELECT * FROM orders WHERE client_id = ? ORDER BY order_date DESC";
        return jdbcTemplate.queryForList(sql, clientId);
    }
}
