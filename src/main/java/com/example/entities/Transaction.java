package com.example.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Transaction {

    private Long transactionId;
    private Long clientId;
    private String type;
    private BigDecimal amount;
    private OffsetDateTime timestamp;

    //Constructor
    public Transaction() {
    }

    public Transaction(
            Long transactionId,
            Long clientId,
            String type,
            BigDecimal amount,
            OffsetDateTime timestamp) {

        this.transactionId = transactionId;
        this.clientId = clientId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }


    //Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
