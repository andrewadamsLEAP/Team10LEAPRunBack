package com.example.entities;

public class Holding {
    private Long client_Id;
    private String ticker;
    private Integer quantity;

    // Getters and Setters
    public Long getClient_Id() {
        return client_Id;
    }

    public void setClient_Id(Long client_Id) {
        this.client_Id = client_Id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
