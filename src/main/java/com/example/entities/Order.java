package com.example.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Order {

    public enum OrderType {
        BUY,
        SELL
    }

    public enum OrderStatus {
        PENDING,
        FULFILLED,
        CANCELLED
    }

    private Long orderId;
    private Long clientId;
    private String ticker;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private int quantity;
    private BigDecimal price;
    private OffsetDateTime orderDate;

    // Constructor
    public Order() {
    }

    public Order(
            Long orderId,
            Long clientId,
            String ticker,
            OrderType orderType,
            OrderStatus orderStatus,
            int quantity,
            BigDecimal price,
            OffsetDateTime orderDate) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.ticker = ticker;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.quantity = quantity;
        this.price = price;
        this.orderDate = orderDate;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OffsetDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(OffsetDateTime orderDate) {
        this.orderDate = orderDate;
    }
}
